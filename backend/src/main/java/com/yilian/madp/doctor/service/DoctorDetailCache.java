package com.yilian.madp.doctor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yilian.madp.doctor.vo.DoctorDetailResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** 【Redis查询优化】Cache Aside：先缓存，未命中查库，再填充；只存公开医生资料。 */
@Component
@RequiredArgsConstructor
public class DoctorDetailCache {
    private static final Logger log = LoggerFactory.getLogger(DoctorDetailCache.class);
    private static final String EMPTY = "__NOT_FOUND__";
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    @Value("${app.doctor-cache.enabled:false}")
    private boolean enabled;
    @Value("${app.doctor-cache.prefix:madp:doctor-detail:v1:}")
    private String prefix;

    public DoctorDetailResponse get(Long doctorId, Supplier<DoctorDetailResponse> loader) {
        if (!enabled) {
            return loader.get();
        }
        String key = prefix + doctorId;
        try {
            String value = redis.opsForValue().get(key);
            if (EMPTY.equals(value)) {
                return null;
            }
            if (value != null) {
                DoctorDetailResponse cached = objectMapper.readValue(value, DoctorDetailResponse.class);
                if (cached != null && doctorId.equals(cached.getId())) {
                    return cached;
                }
                redis.delete(key);
            }
        } catch (RuntimeException | JsonProcessingException exception) {
            // 【故障回源】Redis不可用或数据损坏时仍查询数据库，不把缓存故障变成业务500。
            log.warn("医生详情缓存读取失败，回源数据库：{}", exception.getClass().getSimpleName());
        }
        // loader在try外：数据库自己的异常必须交给原异常处理，不能当成缓存故障吞掉。
        DoctorDetailResponse doctor = loader.get();
        try {
            String value = doctor == null ? EMPTY : objectMapper.writeValueAsString(doctor);
            // 【穿透与过期】不存在/未启用统一空值短缓存；正常值5分钟并加0～60秒抖动。
            long seconds = doctor == null ? 30 : 300 + ThreadLocalRandom.current().nextInt(61);
            redis.opsForValue().set(key, value, Duration.ofSeconds(seconds));
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("医生详情缓存写入失败，保留数据库结果：{}", exception.getClass().getSimpleName());
        }
        return doctor;
    }

    public void evictAfterCommit(Long doctorId) {
        if (!enabled) {
            return;
        }
        // 【更新失效】必须在事务提交后删除；回滚时不删除，避免填回尚未提交的旧状态。
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(doctorId);
                }
            });
        } else {
            evict(doctorId);
        }
    }

    private void evict(Long doctorId) {
        try {
            redis.delete(prefix + doctorId);
        } catch (RuntimeException exception) {
            // 数据库已提交，删缓存失败不能伪装成数据库回滚；TTL兜底，后续可补重试机制。
            log.warn("医生详情缓存失效失败，等待TTL：{}", exception.getClass().getSimpleName());
        }
    }
}
