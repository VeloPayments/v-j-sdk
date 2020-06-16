package com.velopayments.blockchain.sdk.sentinel.criteria;

public class InvalidCriteriaException extends RuntimeException {

    public InvalidCriteriaException(String msg) {
        super(msg);
    }

    public InvalidCriteriaException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
