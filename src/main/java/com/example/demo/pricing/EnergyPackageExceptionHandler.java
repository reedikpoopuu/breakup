package com.example.demo.pricing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminEnergyPackageController.class)
public class EnergyPackageExceptionHandler {

    @ExceptionHandler(EnergyPackageNotFoundException.class)
    public ResponseEntity<String> onNotFound(EnergyPackageNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(EnergyPackageNotEditableException.class)
    public ResponseEntity<String> onNotEditable(EnergyPackageNotEditableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
