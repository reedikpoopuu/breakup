package com.example.demo.common;

/** Thrown by {@link OutboundUrlValidator} when a URL this app would fetch server-side isn't safe to fetch. */
public class UnsafeOutboundUrlException extends RuntimeException {

    public UnsafeOutboundUrlException(String url, String reason) {
        super("Refusing to fetch '" + url + "': " + reason);
    }
}
