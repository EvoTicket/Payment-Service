package com.capstone.paymentservice.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Gửi message tới Redis Stream với retry tự động.
     * Retry tối đa 3 lần, exponential backoff: 1s -> 2s -> 4s.
     * Nếu sau 3 lần vẫn fail, exception sẽ được throw lên caller
     * (PaymentTransactionService sẽ catch và lưu trạng thái FAILED vào DB để retry sau).
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public void sendMessage(String streamKey, Object message) {
        try {
            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("payload", objectMapper.writeValueAsString(message));
            messageMap.put("timestamp", String.valueOf(System.currentTimeMillis()));

            ObjectRecord<String, Map<String, String>> objectRecord = StreamRecords
                    .newRecord()
                    .ofObject(messageMap)
                    .withStreamKey(streamKey);

            redisTemplate.opsForStream().add(objectRecord);

            log.info("Message sent to stream '{}': {}", streamKey, message);
        } catch (Exception e) {
            log.error("Error sending message to stream '{}' (will retry if attempts remain)", streamKey, e);
            throw new RuntimeException("Failed to send message to Redis stream: " + streamKey, e);
        }
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public void sendMessageWithKey(String streamKey, String messageKey, Object message) {
        try {
            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("key", messageKey);
            messageMap.put("payload", objectMapper.writeValueAsString(message));
            messageMap.put("timestamp", String.valueOf(System.currentTimeMillis()));

            ObjectRecord<String, Map<String, String>> objectRecord = StreamRecords
                    .newRecord()
                    .ofObject(messageMap)
                    .withStreamKey(streamKey);

            redisTemplate.opsForStream().add(objectRecord);

            log.info("Message with key '{}' sent to stream '{}': {}", messageKey, streamKey, message);
        } catch (Exception e) {
            log.error("Error sending message to stream '{}' (will retry if attempts remain)", streamKey, e);
            throw new RuntimeException("Failed to send message to Redis stream: " + streamKey, e);
        }
    }
}