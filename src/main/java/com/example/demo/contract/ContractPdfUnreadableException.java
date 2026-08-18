package com.example.demo.contract;

/** The uploaded file couldn't be parsed as a PDF - corrupt, encrypted, too complex, or not a PDF at all. */
public class ContractPdfUnreadableException extends RuntimeException {

    public ContractPdfUnreadableException(Throwable cause) {
        super("Could not read the uploaded file as a PDF", cause);
    }

    public ContractPdfUnreadableException(String message) {
        super(message);
    }
}
