// src/test/java/com/larva/cashback/global/config/TestRedisConfig.java
package com.larva.cashback.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class TestRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void start() throws IOException {
        redisServer = new RedisServer(6380);  // application.yml 포트랑 맞춤
        redisServer.start();
    }

    @PreDestroy
    public void stop() throws IOException {
        redisServer.stop();
    }
}