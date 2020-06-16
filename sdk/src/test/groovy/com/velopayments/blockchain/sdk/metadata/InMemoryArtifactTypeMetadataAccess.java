package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.Signer;
import lombok.val;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.velopayments.blockchain.sdk.metadata.CoreMetadata.CORE_METADATA_TYPE_ID;

@Deprecated
public class InMemoryArtifactTypeMetadataAccess implements ArtifactTypeMetadataAccess {

    private final Map<UUID,ArtifactTypeMetadata> metadataMap = new HashMap<>();

    public InMemoryArtifactTypeMetadataAccess(Signer signer) {
        store(CoreMetadata.create(), signer);
        store(ArtifactTypeType.create(), signer);
    }

    @Override
    public CompletableFuture<TransactionStatus> store(ArtifactTypeMetadataBuilder builder, Signer signer) {
        metadataMap.put(CORE_METADATA_TYPE_ID, CoreMetadata.create().getMetadata());
        metadataMap.put(builder.getMetadata().getArtifactTypeId(), builder.getMetadata());
        return CompletableFuture.completedFuture(TransactionStatus.SUCCEEDED);
    }

    @Override
    public Collection<ArtifactTypeMetadata> listArtifactTypes() {
        return metadataMap.values();
    }

    @Override
    public Optional<ArtifactTypeMetadata> findByArtifactTypeId(UUID artifactTypeId) {
        val typeMetadata = metadataMap.get(artifactTypeId);
        return Optional.ofNullable(typeMetadata);
    }
}
