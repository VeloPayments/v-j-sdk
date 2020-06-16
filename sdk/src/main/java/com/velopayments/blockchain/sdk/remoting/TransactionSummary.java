package com.velopayments.blockchain.sdk.remoting;

import com.velopayments.blockchain.sdk.TransactionReader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummary {

    private UUID transactionId;
    private UUID transactionType;
    private UUID artifactId;
    private UUID artifactType;

    public static TransactionSummary transformTransaction(TransactionReader txReader) {
        return TransactionSummary.builder()
            .transactionId(txReader.getTransactionId())
            .transactionType(txReader.getTransactionType())
            .artifactId(txReader.getArtifactId())
            .artifactType(txReader.getArtifactType())
            .build();
    }
}
