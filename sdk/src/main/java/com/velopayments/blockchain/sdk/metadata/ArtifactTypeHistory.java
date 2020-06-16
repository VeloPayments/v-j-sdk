package com.velopayments.blockchain.sdk.metadata;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;

@Slf4j
public class ArtifactTypeHistory {

    public static final UUID ARTIFACT_TYPE_HISTORY_TYPE_ID = UUID.fromString("329baeb2-68e9-457d-bb37-92ee8e80da2b");
    public static final UUID ARTIFACT_TYPE_HISTORY_ARTIFACT_ID = UUID.fromString("15e95036-5273-4171-9a44-bad0c2bd5cf8");   //this ID allows all artifact types to be found by artifact id

    public static final TransactionType TYPE_STORED = new TransactionType(UUID.fromString("72543cbb-8ca0-4f12-a319-90400829bdae"), "TYPE_STORED");

    public static final FieldMetadata ARTIFACT_TYPE_ID = new FieldMetadata(0x0801, "ARTIFACT_TYPE_ID", FieldType.String, false, false, 1000);

    public static ArtifactTypeMetadataBuilder create() {
        return extractMetadata(ARTIFACT_TYPE_HISTORY_TYPE_ID, "ARTIFACT_TYPE_HISTORY", ArtifactTypeHistory.class);
    }

}
