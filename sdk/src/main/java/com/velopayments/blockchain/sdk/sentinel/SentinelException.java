package com.velopayments.blockchain.sdk.sentinel;

public class SentinelException extends RuntimeException {

    public SentinelException(String message) {
        super(message);
    }

    public SentinelException(String message, Exception e) {
        super(message,e);
    }
}
