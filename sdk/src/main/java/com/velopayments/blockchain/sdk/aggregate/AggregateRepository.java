package com.velopayments.blockchain.sdk.aggregate;

import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.vault.ExternalReference;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface AggregateRepository<T extends Aggregate> {

    BlockchainOperations getBlockchain();

    UUID getArtifactType();

    T supply(UUID artifactId);

    default Optional<T> findAggregate(UUID artifactId) {
        final BlockchainOperations blockchain = getBlockchain();
        return blockchain.findFirstTransactionIdForArtifactById(artifactId)
            .map(initialTransaction -> {
                T aggregate = supply(artifactId);
                blockchain.findTransactionById(initialTransaction)
                    .ifPresent(txn -> aggregate.apply(txn, blockchain.loadExternalReferences(txn)));

                // restore state
                Optional<UUID> transactionId = blockchain.findNextTransactionIdForTransactionById(initialTransaction);
                while (transactionId.isPresent()) {
                    blockchain.findTransactionById(transactionId.get())
                        .ifPresent(txn -> aggregate.apply(txn, blockchain.loadExternalReferences(txn)));
                    transactionId = blockchain.findNextTransactionIdForTransactionById(transactionId.get());
                }
                return aggregate;
            });
    }

    default Stream<ExternalReference> loadExternalReferences(TransactionReader transaction) {
        return getBlockchain().loadExternalReferences(transaction);
    }
}
