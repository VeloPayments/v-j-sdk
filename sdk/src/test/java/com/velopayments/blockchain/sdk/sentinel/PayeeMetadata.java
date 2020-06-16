package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.sdk.metadata.*;

import java.util.UUID;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;

public class PayeeMetadata {
    public static final String PAYEE_TYPE_NAME = "PAYEE";
    public static final UUID PAYEE_TYPE_ID = UUID.fromString("b83f2f1e-31f4-40f4-84f5-2db9fcaadb01");

    public static final TransactionType PAYEE_CREATED = new TransactionType(UUID.fromString("4b812379-ab3d-4029-83d3-83ca43b6c32c"), "PAYEE_CREATED");

    public static final FieldMetadata PAYEE_STRING_FIELD = new FieldMetadata(0x0443, "PAYEE_STRING_FIELD", FieldType.String, false, false, 230);
    public static final FieldMetadata PAYEE_INTEGER_FIELD = new FieldMetadata(0x0444, "PAYEE_INTEGER_FIELD", FieldType.Integer, false, false, 240);

    public static final ArtifactState STATE_NEW = new ArtifactState(100, "NEW");
    public static final ArtifactState STATE_ACTIVE = new ArtifactState(101, "ACTIVE");

    public static ArtifactTypeMetadataBuilder create() {
        return extractMetadata(PAYEE_TYPE_ID, PAYEE_TYPE_NAME, PayeeMetadata.class);
    }
}
