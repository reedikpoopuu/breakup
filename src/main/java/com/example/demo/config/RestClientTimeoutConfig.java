package com.example.demo.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.time.Duration;

/**
 * Applies connect/read timeouts to every {@code RestClient.Builder} obtained via
 * dependency injection - the AI gateway clients, all three national DataHub adapters,
 * and Nord Pool. Without this, none of those outbound calls had any timeout at all: a
 * slow or hanging upstream would block the Tomcat worker thread handling that request
 * indefinitely, which - stacked with the fact that some of these are reachable by any
 * authenticated user, not just admins (contract parsing triggers the AI gateway call) -
 * is a real resource-exhaustion path, not just a hypothetical one.
 */
@Configuration
public class RestClientTimeoutConfig {

    @Bean
    public RestClientCustomizer restClientTimeoutCustomizer() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(20));
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);
        return builder -> builder.requestFactory(factory);
    }
}
