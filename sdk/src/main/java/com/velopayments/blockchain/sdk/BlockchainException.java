package com.velopayments.blockchain.sdk;

public class BlockchainException extends RuntimeException {

    public BlockchainException(Throwable cause) {
        super(cause);
    }

    public BlockchainException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlockchainException(String message) {
        super(message);
    }
}
