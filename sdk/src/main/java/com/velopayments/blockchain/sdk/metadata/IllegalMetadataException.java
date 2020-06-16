package com.velopayments.blockchain.sdk.metadata;

public class IllegalMetadataException extends MetadataException {

    public IllegalMetadataException(String msg) {
        super(msg);
    }

    public IllegalMetadataException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
