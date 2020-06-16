package com.velopayments.blockchain.sdk.aggregate;

import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.vault.ExternalReference;

import java.util.UUID;
import java.util.stream.Stream;

/**
 * An interface to support an artifact that follows an event-sourcing pattern. Aggregate is a domain-driven-design (DDD)
 * concept that fits well within event sourcing. An aggregate can populate (re-hydrate) its state by sequential
 * of an artifacts transactions (treating them as events).
 */
public interface Aggregate {

    /**
     * the artifact id
     * @return a non-null {@code UUID} for the artifact id
     */
    UUID getId();

    /**
     * Apply a transaction to aggregate state.
     * @param transaction the transaction to apply
     */
    default void apply(TransactionReader transaction) {
        apply(transaction, Stream.empty());
    }

    /**
     * Apply a transaction to aggregate state.
     * @param transaction the transaction to apply
     * @param externalReferences a (possibly empty) {@code Stream } external references which
     */
    void apply(TransactionReader transaction, Stream<ExternalReference> externalReferences);

}
