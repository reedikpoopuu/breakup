package com.example.demo.auth;

import com.example.demo.controller.SmartIdAuthController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SmartIdAuthController.class)
public class SmartIdExceptionHandler {

    @ExceptionHandler(SmartIdNotConfiguredException.class)
    public ResponseEntity<String> onNotConfigured(SmartIdNotConfiguredException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
    }
}
