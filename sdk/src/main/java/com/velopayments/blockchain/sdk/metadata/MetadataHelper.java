package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.Signer;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MetadataHelper {

    /**
     * Convenience method to properly create {@link ArtifactTypeMetadata}s in the Blockchain and wait for them to be stored
     */
    public static void initMetadata(BlockchainOperations blockchain, Signer signer, ArtifactTypeMetadataBuilder... metadatas) {
        var metadataAccess = new ArtifactTypeMetadataAccessBlockchain(blockchain);
        Arrays.stream(metadatas).forEach(builder -> {
            try {
                TransactionStatus status = metadataAccess.store(builder, signer).get(20, TimeUnit.SECONDS);
                if (TransactionStatus.SUCCEEDED != status) {
                    throw new MetadataException("Error initialising metadata: " + builder + ".  Got transaction status: " + status);
                }
            }
            catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new MetadataException("Error initialising metadata: " + builder, e);
            }
        });
    }
}
