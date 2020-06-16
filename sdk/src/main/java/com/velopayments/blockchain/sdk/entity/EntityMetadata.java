package com.velopayments.blockchain.sdk.entity;

import com.velopayments.blockchain.sdk.metadata.*;

import java.util.Collections;
import java.util.UUID;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;

public abstract class EntityMetadata {

    private EntityMetadata() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final UUID ARTIFACT_TYPE_ID = UUID.fromString("6b7d89e0-1234-1234-8463-23849d805432");
    public static final String ARTIFACT_TYPE_NAME = "ENTITY";

    public static final FieldMetadata ENTITY_NAME = new FieldMetadata(0x0510, "ENTITY_NAME", FieldType.String, false, false, 1000);

    public static final TransactionType ENTITY_CREATED = new TransactionType(UUID.fromString("0ae8f08b-56ed-41a8-975e-0d2b70a5da98"), "ENTITY_CREATED");

    //redefine DISPLAY_NAME as a calculated field
    public static final FieldMetadata DISPLAY_NAME = CoreMetadata.DISPLAY_NAME.copy().setCalculatedValue(Collections.singletonList(new FieldValueSource(ENTITY_NAME.getId())));

    public static ArtifactTypeMetadataBuilder create() {
        return extractMetadata(ARTIFACT_TYPE_ID, ARTIFACT_TYPE_NAME, EntityMetadata.class)
            .withSearchOptions(SearchOptions.FullTextNonEncryptedFieldValues);
    }
}

