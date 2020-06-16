package com.velopayments.blockchain.sdk.sentinel;

public class SentinelConfigException extends SentinelException {
    public SentinelConfigException(String message) {
        super(message);
    }

    public SentinelConfigException(String name, Object value) {
        this(name, value, null);
    }

    public SentinelConfigException(String name, Object value, String message) {
        super("Invalid value " + value + " for configuration " + name + (message == null ? "" : ": " + message));
    }
}
