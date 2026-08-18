package com.example.demo.supplier;

import com.example.demo.common.UnsafeOutboundUrlException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AdminSupplierController.class, PublicSupplierController.class})
public class SupplierExceptionHandler {

    @ExceptionHandler(DuplicateSupplierException.class)
    public ResponseEntity<String> onDuplicate(DuplicateSupplierException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<String> onNotFound(SupplierNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(UnsafeOutboundUrlException.class)
    public ResponseEntity<String> onUnsafeUrl(UnsafeOutboundUrlException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
