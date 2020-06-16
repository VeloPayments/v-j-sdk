package com.velopayments.blockchain.sdk.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionType implements Comparable<TransactionType> {

    private UUID id;

    /**
     * This must be upper case alphanumeric  ^[_a-zA-Z0-9]+$
     * with no spaces - please use underscores instead of spaces, just like Java constants
     */
    private String name;

    @Override
    public int compareTo(TransactionType other) {
        return id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return name;
    }
}
