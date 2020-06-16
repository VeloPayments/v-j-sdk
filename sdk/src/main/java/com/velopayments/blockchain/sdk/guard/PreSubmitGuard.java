package com.velopayments.blockchain.sdk.guard;

import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.aggregate.GuardedTransactionException;

/**
 * An interface for a guard which can prevent transactions for being submitted to the blockchain.
 */
public interface PreSubmitGuard {

    /**
     * Evaluate the given transaction.
     * @param transaction a transaction to evaulate
     * @param blockchain a {@code BlockchainOperations} object being used to store the transaction
     * @throws GuardedTransactionException the transaction violates the constraints defined by the guard
     */
    void evaluate(TransactionReader transaction, BlockchainOperations blockchain);
}
