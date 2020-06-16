package com.velopayments.blockchain.sdk.metadata;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;

@Slf4j
public class ArtifactTypeType {

    public static final UUID ARTIFACT_TYPE_TYPE_TYPE_ID = UUID.fromString("ac6b4636-bc10-449f-b647-777777777777");

    public static final TransactionType TYPE_CREATED = new TransactionType(UUID.fromString("8fe39afd-8987-4e7d-a7a5-1ce33c47ecb6"), "TYPE_CREATED");
    public static final TransactionType TYPE_UPDATED = new TransactionType(UUID.fromString("b8b37917-76ab-4940-900a-20c566b62896"), "TYPE_UPDATED");

    public static final FieldMetadata TYPE_METADATA_JSON = new FieldMetadata(0x05000, "TYPE_METADATA_JSON", FieldType.String, false, false, 500);

    public static ArtifactTypeMetadataBuilder create() {
        return extractMetadata(ARTIFACT_TYPE_TYPE_TYPE_ID, "ARTIFACT_TYPE", ArtifactTypeType.class);
    }

}
