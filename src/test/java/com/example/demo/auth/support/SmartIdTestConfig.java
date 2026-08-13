package com.example.demo.auth.support;

import com.example.demo.auth.SmartIdService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class SmartIdTestConfig {

    @Bean
    @Primary
    public SmartIdService smartIdService() {
        return new FakeSmartIdService();
    }
}
