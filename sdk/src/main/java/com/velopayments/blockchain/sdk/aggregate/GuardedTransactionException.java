package com.velopayments.blockchain.sdk.aggregate;

import com.velopayments.blockchain.sdk.BlockchainException;

public class GuardedTransactionException extends BlockchainException {
    public GuardedTransactionException(Throwable cause) {
        super(cause);
    }

    public GuardedTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public GuardedTransactionException(String message) {
        super(message);
    }
}
