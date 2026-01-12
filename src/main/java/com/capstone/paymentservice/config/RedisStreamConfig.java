package com.capstone.paymentservice.config;

import com.capstone.paymentservice.exception.AppException;
import com.capstone.paymentservice.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;

@Configuration
public class RedisStreamConfig {
    @Value("${spring.data.redis.url:redis://localhost:6379}")
    private String redisUrl;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConfigData data = parseRedisUrl(redisUrl);

        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(data.host(), data.port());

        if (data.username() != null) config.setUsername(data.username());
        if (data.password() != null) config.setPassword(data.password());

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder();

        if (data.useSsl()) builder.useSsl();

        return new LettuceConnectionFactory(config, builder.build());
    }

    private RedisConfigData parseRedisUrl(String redisUrl) {
        if (redisUrl == null || redisUrl.isEmpty()) {
            return new RedisConfigData("localhost", 6379, null, null, false);
        }

        try {
            boolean useSsl = redisUrl.startsWith("rediss://");
            redisUrl = redisUrl.replaceFirst("rediss?://", "");

            String username = null;
            String password = null;
            String host;
            int port;

            String[] authAndHost = redisUrl.split("@");

            if (authAndHost.length == 2) {
                String[] credentials = authAndHost[0].split(":");
                username = credentials[0];
                password = credentials.length > 1 ? credentials[1] : null;

                String[] hostPort = authAndHost[1].split(":");
                host = hostPort[0];
                port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 6379;
            }

            else {
                String[] hostPort = authAndHost[0].split(":");
                host = hostPort[0];
                port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 6379;
            }

            return new RedisConfigData(host, port, username, password, useSsl);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Invalid Redis URL format: " + redisUrl, e);
        }
    }


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
        return template;
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory factory) {

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(100))
                        .build();

        return StreamMessageListenerContainer.create(factory, options);
    }
}