package com.velopayments.blockchain.sdk.sentinel;

public class SentinelRegistryException extends RuntimeException {

    public SentinelRegistryException(String msg) {
        super(msg);
    }

    public SentinelRegistryException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
