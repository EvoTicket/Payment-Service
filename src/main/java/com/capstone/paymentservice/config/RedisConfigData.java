package com.capstone.paymentservice.config;

public record RedisConfigData(String host, int port, String username, String password, boolean useSsl) {}

