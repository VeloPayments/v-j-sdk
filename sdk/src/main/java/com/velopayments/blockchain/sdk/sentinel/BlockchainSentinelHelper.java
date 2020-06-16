package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.sdk.BlockReader;
import com.velopayments.blockchain.sdk.BlockchainException;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.TransactionReader;
import lombok.RequiredArgsConstructor;

import java.util.*;

import static com.velopayments.blockchain.cert.CertificateType.ROOT_BLOCK;
import static com.velopayments.blockchain.sdk.BlockchainUtils.INITIAL_TRANSACTION_UUID;

/**
 * Helper methods to assist Sentinels
 */
@RequiredArgsConstructor
public class BlockchainSentinelHelper {

    private final BlockchainOperations blockchain;

    public QueryResult<List<TransactionReader>> findTransactionsForArtifact(UUID artifactId) {
        return findTransactionsForArtifact(artifactId, null);
    }

    /**
     * Find the transactions for an artifact up until a block id
     *
     * Because Sentinels get notified of invalidation events rather then notified of new transactions, they need
     *  to be able to query with a known end point in the blockchain.  This method helps that
     * @return the {@link QueryResult} with the transactions in ascending order
     */
    public QueryResult<List<TransactionReader>> findTransactionsForArtifact(UUID artifactId, UUID untilBlockId) {
        UUID latestBlockId = untilBlockId == null ? blockchain.getLatestBlockId() : untilBlockId;
        long maxBlockHeightForQuery = getBlockHeight(latestBlockId);

        List<TransactionReader> transactions = new ArrayList<>();

        Optional<UUID> lastTransactionId = blockchain.findLastTransactionIdForArtifactById(artifactId);
        if (lastTransactionId.isPresent()) {
            UUID transactionId = lastTransactionId.get();
            while (true) {
                long blockHeight = blockchain.findTransactionBlockId(transactionId)
                    .map(this::getBlockHeight)
                    .orElseThrow(() ->  new BlockchainException("Block for transaction id: " + lastTransactionId.get() + " not found"));

                final UUID previousTxId;
                if (blockHeight > maxBlockHeightForQuery) {
                    //ignore this transaction - it is too recent
                    Optional<UUID> prev = blockchain.findPreviousTransactionIdForTransactionById(transactionId);
                    if (prev.isPresent()) {
                        previousTxId = prev.get();
                    } else {
                        break;  //we reached the root
                    }
                }
                else {
                    TransactionReader txnReader = blockchain.findTransactionById(transactionId)
                        .orElseThrow(() ->  new BlockchainException("Transaction for id: " + lastTransactionId.get() + " not found"));
                    transactions.add(txnReader);
                    previousTxId = txnReader.getPreviousTransactionId();
                }

                if (INITIAL_TRANSACTION_UUID.equals(previousTxId)) {
                    break;  //we reached the root
                }

                transactionId = previousTxId;       //go loop again for the previous transaction
            }
        }

        //TODO: replace the need to reverse
        Collections.reverse(transactions);  //return the transactions in ascending order

        return QueryResult.<List<TransactionReader>>builder()
            .result(transactions)
            .latestBlockId(latestBlockId)
            .build();
    }

    private long getBlockHeight(UUID blockId) {
        if (blockId.equals(ROOT_BLOCK)) {
            return 0;
        }
        //TODO source this from the cache
        BlockReader blockReader = blockchain.findBlockById(blockId).orElseThrow(() -> new BlockchainException("Couldn't find block " + blockId));
        return blockReader.getBlockHeight();
    }
}
