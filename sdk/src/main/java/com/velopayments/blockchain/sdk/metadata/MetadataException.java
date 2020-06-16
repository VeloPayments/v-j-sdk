package com.velopayments.blockchain.sdk.metadata;

public class MetadataException extends RuntimeException {

    public MetadataException(String msg) {
        super(msg);
    }

    public MetadataException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
