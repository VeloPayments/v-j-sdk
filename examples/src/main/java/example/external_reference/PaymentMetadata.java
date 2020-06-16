package example.external_reference;

import com.velopayments.blockchain.sdk.metadata.*;

import java.util.Collections;
import java.util.UUID;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;

/**
 * Blockchain Metatdata for an example Payment artifact
 */
public class PaymentMetadata {

    public static final String PAYMENT_TYPE_NAME = "PAYMENT";
    public static final UUID PAYMENT_TYPE_ID = UUID.fromString("904bdb45-62c2-43cd-8e48-5bc4a15745e8");

    public static final FieldMetadata ITEM_DESCRIPTION     = new FieldMetadata(0x0400, "ITEM_DESCRIPTION", FieldType.String, false, false, 130);
    public static final FieldMetadata CUSTOMER_EMAIL       = new FieldMetadata(0x0401, "CUSTOMER_EMAIL", FieldType.String, false, false, 135);
    public static final FieldMetadata CUSTOMER_NAME        = new FieldMetadata(0x0402, "CUSTOMER_NAME", FieldType.String, false, false, 140);
    public static final FieldMetadata PAYMENT_AMOUNT       = new FieldMetadata(0x0403, "PAYMENT_AMOUNT", FieldType.String, false, false, 145);
    public static final FieldMetadata PAYMENT_DOCUMENT     = new FieldMetadata(0x0414, "PAYMENT_DOCUMENT", FieldType.ExternalReference, false, false, 260);
    public static final FieldMetadata DISPLAY_NAME         = CoreMetadata.DISPLAY_NAME.copy().setCalculatedValue(Collections.singletonList(new FieldValueSource(ITEM_DESCRIPTION.getId())));

    public static final TransactionType CREATED = new TransactionType(UUID.fromString("3ba7c8da-4372-4e61-9d3b-efd0db04c4dd"), "CREATED");
    public static final TransactionType UPDATED = new TransactionType(UUID.fromString("112fce19-3a93-4c9b-9ac0-372dc0f771dd"), "UPDATED");

    public static final ArtifactState STATE_INSTRUCTED = new ArtifactState(101, "INSTRUCTED");
    public static final ArtifactState STATE_COMPLETE = new ArtifactState(102, "COMPLETE");
    public static final ArtifactState STATE_FAILED = new ArtifactState(103, "FAILED");

    /**
     * Storing {@link ArtifactTypeMetadata} means that you can use VeloChain Explorer to visualise the stored data
     * @return a new metadata builder instance
     */
    public static ArtifactTypeMetadataBuilder create() {
        return extractMetadata(PAYMENT_TYPE_ID, PAYMENT_TYPE_NAME, PaymentMetadata.class).withSearchOptions(SearchOptions.FullTextNonEncryptedFieldValues);
    }
}
