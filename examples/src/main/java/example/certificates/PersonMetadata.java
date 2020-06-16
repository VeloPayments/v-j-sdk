package example.certificates;

import com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder;
import com.velopayments.blockchain.sdk.metadata.FieldMetadata;
import com.velopayments.blockchain.sdk.metadata.FieldType;
import com.velopayments.blockchain.sdk.metadata.TransactionType;

import java.util.UUID;

public class PersonMetadata {

    public static final String PERSON_TYPE_NAME = "PERSON";
    public static final UUID ARTIFACT_TYPE_ID = UUID.fromString("ba9a5b67-0fc0-4972-84ec-8a353fe9e714");

    public static final FieldMetadata FIRST_NAME_FIELD = new FieldMetadata(0x0800, "FIRST_NAME", FieldType.String, false, false, 100);
    public static final FieldMetadata LAST_NAME_FIELD = new FieldMetadata(0x0801, "LAST_NAME", FieldType.String, false, false, 101);
    public static final FieldMetadata DOB_FIELD  = new FieldMetadata(0x0802, "DOB", FieldType.Date, false, false, 102);
    // encrypted fields..
    public static final FieldMetadata ACCOUNT_NUM_FIELD = new FieldMetadata(0x0803, "ACCOUNT_NUMBER", FieldType.String, true, false, 150);
    public static final FieldMetadata LUCKY_NUMBER_FIELD  = new FieldMetadata(0x0804, "LUCKY_NUMBER", FieldType.Integer, true, false, 151);

    public static final TransactionType PERSON_CREATED = new TransactionType(UUID.fromString("ec392f64-d424-442c-a911-c8f81de8267a"), "PERSON_CREATED");
    public static final TransactionType PERSON_UPDATED = new TransactionType(UUID.fromString("e2d27110-bef8-491e-9f0c-5e1095d550cf"), "PERSON_UPDATED");

    public static ArtifactTypeMetadataBuilder create() {
        return ArtifactTypeMetadataBuilder.extractMetadata(ARTIFACT_TYPE_ID, PERSON_TYPE_NAME, PersonMetadata.class);
    }
}
