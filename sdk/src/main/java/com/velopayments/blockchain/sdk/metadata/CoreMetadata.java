package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.sdk.vault.VaultUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.UUID;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;
import static com.velopayments.blockchain.sdk.metadata.FieldType.Date;

@Slf4j
public class CoreMetadata {

    public static final UUID CORE_METADATA_TYPE_ID = UUID.fromString("ac6b4636-bc10-449f-b647-555555555555");
    public static final String CORE_METADATA_TYPE_NAME = "CORE_METADATA";

    /**
     * Not really for setting on certs - just want to provide field metadata so the transaction detail transformer can handle it.  Ties together with TransactionUtils.DISPLAY_NAME
     */
    public static final FieldMetadata DISPLAY_NAME                             = new FieldMetadata(0x0507, "DISPLAY_NAME", FieldType.String, false, false, 1000, true);

    public static final FieldMetadata CREATED_DATE                             = new FieldMetadata(0x0508, "CREATED_DATE", Date, false, false, 140);    //TODO DateTime BP-131

    public static final FieldMetadata UPDATED_DATE                             = new FieldMetadata(0x0509, "UPDATED_DATE", Date, false, false, 150);    //TODO DateTime BP-131

    //TODO this is only really temporary until we support data dictionaries
    public static final FieldMetadata TEST_ENCRYPTED_VALUE                     = new FieldMetadata(0x0511, "TEST_ENCRYPTED_VALUE", FieldType.String, true, false, 900);


    //TODO: standard fields? Maybe we don't use EXTERNAL_REF_SIGNED
    // External References

    /**
     * A field that a transaction will use to hold external reference a signed by the holder of the data to verify the data is stored.
     */
    public static final FieldMetadata EXTERNAL_REF_SIGNED = new FieldMetadata(Field.VELO_RESERVED_00B4, "EXTERNAL_REF_SIGNED", FieldType.ByteArray, false, false, 1000);

    /**
     * A field that a transaction will use to hold an external reference.
     */
    public static final FieldMetadata EXTERNAL_REF = new FieldMetadata(Field.VELO_RESERVED_00B5, "EXTERNAL_REF", FieldType.ByteArray, false, false, 1000);

    /**
     * A field that identifies the type of external reference. This may signal a different certificate schema or storage mechanism.
     */
    public static final FieldMetadata EXTERNAL_REF_TYPE = new FieldMetadata(VaultUtils.EXTERNAL_REF_TYPE, "EXTERNAL_REF_TYPE", FieldType.UUID, false, false, 1000);

    /**
     * A field that provided a unique identifier for an external reference.
     */
    public static final FieldMetadata EXTERNAL_REF_ID =  new FieldMetadata(Field.VELO_RESERVED_00B7, "EXTERNAL_REF_ID", FieldType.UUID, false, false, 1000);

    /**
     * A field that identifies the media (MIME) type of the referenced content, as defined in RFC 2045 and 2046.
     */
    public static final FieldMetadata EXTERNAL_REF_CONTENT_MEDIA_TYPE = new FieldMetadata(Field.VELO_RESERVED_00B8, "EXTERNAL_REF_CONTENT_MEDIA_TYPE", FieldType.String, false, false, 1000);

    /**
     * A field that identifies the length of the referenced content in 8-bit bytes.
     */
    public static final FieldMetadata EXTERNAL_REF_CONTENT_LENGTH = new FieldMetadata(Field.VELO_RESERVED_00B9, "EXTERNAL_REF_CONTENT_LENGTH", FieldType.Long, false, false, 1000);

    /**
     * A field for the encrypted shared secret for each recipient of the external reference (as per encrypted fields), signed by the encrypting user
     */
    public static final FieldMetadata EXTERNAL_REF_SHARED_SECRET_RECIPIENT =  new FieldMetadata(Field.VELO_RESERVED_00BA, "EXTERNAL_REF_SHARED_SECRET_RECIPIENT", FieldType.ByteArray, true, false, 1000);

    /**
     *  A field that holds a signed the encrypted shared secret, signed by the encrypting user. One field for each recipient.
     */
    public static final FieldMetadata EXTERNAL_REF_SIGNATURE =  new FieldMetadata(Field.VELO_RESERVED_00BB, "EXTERNAL_REF_SIGNATURE", FieldType.ByteArray, false, false, 1000);

    /**
     * A field that holds the id of another field from the artifact which the data will be referenced by.
     */
    public static final FieldMetadata EXTERNAL_REF_ARTIFACT_FIELD_ID = new FieldMetadata(Field.VELO_RESERVED_00BC, "EXTERNAL_REF_ARTIFACT_FIELD_ID", FieldType.Integer, false, false, 1000);


    /**
     * A field that holds a file name which may have be used for the file before it was held as an external reference. It is just meta-data and is not used to track or store the file.
     */
    public static final FieldMetadata EXTERNAL_REF_ORIG_FILE_NAME =  new FieldMetadata(Field.VELO_RESERVED_00BD, "EXTERNAL_REF_ORIG_FILE_NAME", FieldType.String, false, false, 1000);

    public static final ArtifactState VOID_STATE = new ArtifactState(0xFFFF,"_VOID");

    public static final ArtifactState CREATED_STATE = new ArtifactState(0x0000,"_CREATED");

    public static ArtifactTypeMetadataBuilder create() {
        val builder = extractMetadata(CORE_METADATA_TYPE_ID, CORE_METADATA_TYPE_NAME, CoreMetadata.class);
        VJBlockchainFields.getStandardFieldMetadata().values().forEach(builder::addField);
        return builder;
    }
}
