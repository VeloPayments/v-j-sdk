package com.velopayments.blockchain.sdk.metadata;

import java.util.Optional;
import java.util.UUID;

/**
 * Just like a Java classloader but it loads {@link ArtifactTypeMetadata}s
 */
public interface ArtifactTypeMetadataLoader {

    /**
     * Find {@link ArtifactTypeMetadata} by the artifactTypeId
     */
    Optional<ArtifactTypeMetadata> findByArtifactTypeId(UUID artifactTypeId);

}
