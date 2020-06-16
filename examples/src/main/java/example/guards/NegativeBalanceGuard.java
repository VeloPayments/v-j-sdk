package example.guards;

import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.guard.PreSubmitGuard;

import static example.guards.GuardExample.GuardExampleMetadata.BALANCE_FIELD;

public class NegativeBalanceGuard implements PreSubmitGuard {

    /**
     * Ensure that the balance is non-negative.
     *
     * @param transaction a transaction to be evaluated for a negative balance
     * @param blockchain a non-null BlockchainOperations instance
     *
     * @throws NegativeBalanceException if the balance is negative
     */
    @Override
    public void evaluate(TransactionReader transaction, BlockchainOperations blockchain) {
        long balance = transaction.getFirst(BALANCE_FIELD.getId()).asLong();
        if (balance < 0L) {
            throw new NegativeBalanceException("Negative balances are not allowed.  Balance=" + balance);
        }
    }
}
