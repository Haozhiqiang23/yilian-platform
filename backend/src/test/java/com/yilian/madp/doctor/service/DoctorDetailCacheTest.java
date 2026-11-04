package com.yilian.madp.doctor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yilian.madp.doctor.vo.DoctorDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorDetailCacheTest {
    StringRedisTemplate redis;
    ValueOperations<String, String> values;
    DoctorDetailCache cache;
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        cache = new DoctorDetailCache(redis, new ObjectMapper());
        ReflectionTestUtils.setField(cache, "enabled", true);
        ReflectionTestUtils.setField(cache, "prefix", "test:");
    }
    @AfterEach
    void cleanup() {
        TransactionSynchronizationManager.clear();
    }
    @Test
    void hitAvoidsDatabase() {
        when(values.get("test:1")).thenReturn("{\"id\":1,\"name\":\"测试医生\"}");
        DoctorDetailResponse result = cache.get(1L, () -> { throw new AssertionError("不应查库"); });
        assertEquals("测试医生", result.getName());
    }
    @Test
    void missingDoctorHasShortNegativeCache() {
        assertNull(cache.get(99L, () -> null));
        verify(values).set("test:99", "__NOT_FOUND__", Duration.ofSeconds(30));
        when(values.get("test:99")).thenReturn("__NOT_FOUND__");
        assertNull(cache.get(99L, () -> { throw new AssertionError("空缓存不应查库"); }));
    }
    @Test
    void redisOutageStillReturnsDatabaseResult() {
        when(values.get(anyString())).thenThrow(new IllegalStateException("redis unavailable"));
        DoctorDetailResponse doctor = new DoctorDetailResponse();
        doctor.setId(1L);
        assertSame(doctor, cache.get(1L, () -> doctor));
    }
    @Test
    void databaseErrorIsNotSwallowed() {
        assertThrows(IllegalArgumentException.class,
                () -> cache.get(1L, () -> { throw new IllegalArgumentException("database failure"); }));
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }
    @Test
    void evictionWaitsForCommit() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        cache.evictAfterCommit(1L);
        verify(redis, never()).delete(anyString());
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(redis).delete("test:1");
    }
    @Test
    void rollbackDoesNotEvict() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        cache.evictAfterCommit(1L);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(1));
        verify(redis, never()).delete(anyString());
    }
    @Test
    void invalidJsonFallsBackAndRefreshes() {
        when(values.get("test:1")).thenReturn("broken");
        AtomicInteger calls = new AtomicInteger();
        cache.get(1L, () -> { calls.incrementAndGet(); return null; });
        assertEquals(1, calls.get());
        verify(values).set("test:1", "__NOT_FOUND__", Duration.ofSeconds(30));
    }
}
