package com.velopayments.blockchain.sdk.aggregate;

import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.guard.PreSubmitGuard;

import java.util.Objects;
import java.util.UUID;

public abstract class AggregateGuard<T extends Aggregate> implements PreSubmitGuard {

    private final AggregateRepository<T> repository;

    public AggregateGuard(AggregateRepository<T> aggregateRepository) {
        this.repository = Objects.requireNonNull(aggregateRepository);
    }

    abstract protected boolean applies(TransactionReader reader);

    @Override
    public void evaluate(TransactionReader reader, BlockchainOperations blockchain) {
        if (supportedByRepository(reader) && applies(reader)) {
            UUID transactionType = reader.getTransactionType();
            UUID artifactId = reader.getArtifactId();
            Aggregate aggregate = repository.findAggregate(artifactId).orElse(repository.supply(artifactId));
            // evaluate this certificate
            try {
                aggregate.apply(reader, repository.loadExternalReferences(reader));
            } catch (Exception ex) {
                throw new GuardedTransactionException(String.format("AggregateGuard cannot apply transaction type %s for artifact %s", transactionType, artifactId), ex);
            }
        }
    }

    protected boolean supportedByRepository(TransactionReader reader) {
        return repository.getArtifactType().equals(reader.getArtifactType());
    }
}
