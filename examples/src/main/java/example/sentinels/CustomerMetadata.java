package example.sentinels;

import com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder;
import com.velopayments.blockchain.sdk.metadata.FieldMetadata;
import com.velopayments.blockchain.sdk.metadata.FieldType;
import com.velopayments.blockchain.sdk.metadata.TransactionType;

import java.util.UUID;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;

public class CustomerMetadata {

    public static final String CUSTOMER_TYPE_NAME = "CUSTOMER";

    // Artifact
    public static final UUID CUSTOMER_ARTIFACT_TYPE_ID = UUID.fromString("79622eb9-87ff-4c5b-a4e7-a356570de6ea");

    // Fields
    public static final FieldMetadata FIRST_NAME          = new FieldMetadata(0x0414, "FIRST_NAME", FieldType.String, false, false, 110);
    public static final FieldMetadata LAST_NAME           = new FieldMetadata(0x0415, "LAST_NAME", FieldType.String, false, false, 120);


    // Transactions
    public static final TransactionType CUSTOMER_CREATED_TRANSACTION = new TransactionType(UUID.fromString("1a189d57-dbeb-4ac5-a62d-91410c0d6eb6"), "CUSTOMER_CREATED");
    public static final TransactionType CUSTOMER_WELCOME_TRANSACTION = new TransactionType(UUID.fromString("5ef0c305-b868-4bb4-8405-5cfdf3375967"), "CUSTOMER_WELCOMED");


    public static ArtifactTypeMetadataBuilder create() {
        return extractMetadata(CUSTOMER_ARTIFACT_TYPE_ID, CUSTOMER_TYPE_NAME, CustomerMetadata.class);
    }
}
