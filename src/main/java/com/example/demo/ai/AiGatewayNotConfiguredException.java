package com.example.demo.ai;

/** Thrown instead of a raw HTTP error when no AI gateway is configured yet. */
public class AiGatewayNotConfiguredException extends RuntimeException {

    public AiGatewayNotConfiguredException() {
        super("AI gateway is not configured - set app.ai.gateway.base-url / api-key / model");
    }
}
