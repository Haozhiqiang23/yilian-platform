package com.yilian.madp.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordEncoderTest {

    @Test
    void shouldEncodePassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "123456";

        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("加密后的密码：" + encodedPassword);
//        $2a$10$M23dEQ8cuun14pgMW5pjCuRKRTUpqtJqspwOxMhwJZFWQHIfPyaJi

        assertTrue(encoder.matches(rawPassword, encodedPassword));
    }
}
