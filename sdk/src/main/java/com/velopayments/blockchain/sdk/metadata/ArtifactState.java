package com.velopayments.blockchain.sdk.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactState implements Comparable<ArtifactState> {

    private int value;
    private String name;

    @Override
    public int compareTo(ArtifactState other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return name;
    }
}
