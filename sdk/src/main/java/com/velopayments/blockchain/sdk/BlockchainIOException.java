package com.velopayments.blockchain.sdk;

public class BlockchainIOException extends BlockchainException {

    public BlockchainIOException(String message) {
        super(message);
    }

    public BlockchainIOException(Exception e) {
        super(e);
    }

    public BlockchainIOException(String message, Throwable cause) {
        super(message, cause);
    }

}
