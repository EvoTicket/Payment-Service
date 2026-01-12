package com.capstone.paymentservice.config;

import vn.payos.PayOS;
import vn.payos.core.ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayOSConfig {
    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Value("${payos.log-level}")
    private String logLevel;

    @Bean
    public PayOS payOS() {
        ClientOptions options =
                ClientOptions.builder()
                        .clientId(clientId)
                        .apiKey(apiKey)
                        .checksumKey(checksumKey)
                        .logLevel(ClientOptions.LogLevel.valueOf(logLevel.toUpperCase()))
                        .build();
        return new PayOS(options);
    }
}
