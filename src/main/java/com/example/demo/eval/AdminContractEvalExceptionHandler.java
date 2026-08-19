package com.example.demo.eval;

import com.example.demo.contract.ContractPdfUnreadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminContractEvalController.class)
public class AdminContractEvalExceptionHandler {

    @ExceptionHandler(ContractPdfUnreadableException.class)
    public ResponseEntity<String> onUnreadable(ContractPdfUnreadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> onBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
