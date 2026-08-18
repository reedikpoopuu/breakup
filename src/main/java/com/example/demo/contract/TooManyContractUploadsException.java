package com.example.demo.contract;

/** Thrown when a caller exceeds {@link ContractParseRateLimiter}'s per-user quota. */
public class TooManyContractUploadsException extends RuntimeException {

    public TooManyContractUploadsException() {
        super("Too many contract uploads - please wait a minute and try again");
    }
}
