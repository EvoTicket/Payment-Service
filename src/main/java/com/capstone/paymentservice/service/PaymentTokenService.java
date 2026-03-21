package com.capstone.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_PREFIX = "payment:token:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    /**
     * Sinh token tạm thời, lưu vào Redis kèm orderCode.
     * Token có hiệu lực 5 phút và chỉ dùng được 1 lần.
     */
    public String generateToken(String orderCode) {
        String token = UUID.randomUUID().toString();
        String key = TOKEN_PREFIX + token;

        redisTemplate.opsForValue().set(key, orderCode, TOKEN_TTL);

        log.debug("Generated payment token for orderCode={}, ttl={}s", orderCode, TOKEN_TTL.getSeconds());
        return token;
    }

    /**
     * Validate và consume token (xóa sau khi dùng - one-time use).
     * Trả về orderCode nếu token hợp lệ, null nếu không hợp lệ hoặc đã hết hạn.
     */
    public String validateAndConsumeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String key = TOKEN_PREFIX + token;
        Object value = redisTemplate.opsForValue().getAndDelete(key);

        if (value == null) {
            log.warn("Invalid or expired payment token: {}", token);
            return null;
        }

        String orderCode = value.toString();
        log.debug("Payment token consumed for orderCode={}", orderCode);
        return orderCode;
    }
}
