package com.example.demo.ai;

/** Thrown instead of a raw HTTP error when no AI gateway is configured yet. */
public class AiGatewayNotConfiguredException extends RuntimeException {

    public AiGatewayNotConfiguredException() {
        this("set app.ai.gateway.base-url / api-key / model");
    }

    public AiGatewayNotConfiguredException(String configurationHint) {
        super("AI gateway is not configured - " + configurationHint);
    }
}
