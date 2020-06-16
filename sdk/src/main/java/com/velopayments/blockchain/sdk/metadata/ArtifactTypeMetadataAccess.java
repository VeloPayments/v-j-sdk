package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.Signer;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ArtifactTypeMetadataAccess extends ArtifactTypeMetadataLoader {

    /**
     * Store {@link ArtifactTypeMetadata} in the blockchain
     *
     * If signingEntityId and signingPrivateKey are provided then the artifact type certificate will be signed
     * @return CompletableFuture holding the transaction result
     */
    CompletableFuture<TransactionStatus> store(ArtifactTypeMetadataBuilder builder, Signer signer);


    default boolean isSystemType(UUID artifactTypeId) {
        return
            CoreMetadata.CORE_METADATA_TYPE_ID.equals(artifactTypeId) ||
            ArtifactTypeHistory.ARTIFACT_TYPE_HISTORY_TYPE_ID.equals(artifactTypeId) ||
            ArtifactTypeType.ARTIFACT_TYPE_TYPE_TYPE_ID.equals(artifactTypeId);
    }

    /**
     * Find all stored {@link ArtifactTypeMetadata}s
     * @return unsorted collection of ArtifactTypeMetadata
     */
    Collection<ArtifactTypeMetadata> listArtifactTypes();

}
