package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.*;
import lombok.Builder;
import lombok.Singular;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * A utility class used to support the creation of external references.
 * <p>
 * Use the external reference builder function to construct an external reference {@code Certificate}, e.g.
 * <pre>
 * {@code
 * Certificate externalReference = VaultUtils.externalReferenceBuilder()
 *     .referenceId(randomUUID())
 *     .artifactId(paymentArtifactId)
 *     .anchorField(PAYMENT_DOCUMENT_REF.getId())
 *     .contentType("image/png")
 *     .contentLength((long) imageBytes.length)
 *     .signature(imageSignature)
 *     .withFields()
 *     .addString(CoreMetadata.EXTERNAL_REF_ORIG_FILE_NAME.getId(), fileName)
 *     .sign(entityId, signingPrivateKey);
 * }
 * </pre>
 */
public final class VaultUtils {

    public static final UUID VAULT_EXTERNAL_REF_TYPE_ID = UUID.fromString("c5e31a70-0707-49ad-a154-23bd9bebdebe");

    private VaultUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Builder
     */
    @Builder(builderMethodName = "externalReferenceBuilder", buildMethodName = "withFields")
    private static CertificateBuilder createExternalReference(UUID referenceId,
                                                              UUID artifactId,
                                                              Integer anchorField,
                                                              byte[] signature,
                                                              String contentType,
                                                              Long contentLength,
                                                              @Singular Collection<Certificate> sharedSecrets) {

        //FIXME: this was being extra-picky about validation to avoid persisting unsupported content-types
//        try {
//             new MimeType(Objects.requireNonNull(contentType, "Content type is required"));
//        } catch (MimeTypeParseException e) {
//            throw new IllegalArgumentException("Invalid content type: " + contentType, e);
//        }
        Objects.requireNonNull(contentType, "Content type is required");

        byte[] copiedSignature = Arrays.copyOf(Objects.requireNonNull(signature, "Signature required"), signature.length);
        CertificateBuilder builder = CertificateBuilder.createCertificateFragmentBuilder()
            .addUUID(VaultUtils.EXTERNAL_REF_TYPE, VAULT_EXTERNAL_REF_TYPE_ID)
            .addUUID(Field.ARTIFACT_ID, Objects.requireNonNull(artifactId, "Artifact id required"))
            .addUUID(VaultUtils.EXTERNAL_REF_ID, Objects.requireNonNull(referenceId, "Reference id required"))
            .addByteArray(VaultUtils.EXTERNAL_REF_SIGNATURE, copiedSignature)
            .addString(VaultUtils.EXTERNAL_REF_CONTENT_MEDIA_TYPE, contentType)
            .addInt(VaultUtils.EXTERNAL_REF_ANCHOR_FIELD_ID, Objects.requireNonNull(anchorField, "Anchor field is required"));

        if (sharedSecrets != null) {
            for (Certificate ss : sharedSecrets) {
                builder = builder.addByteArray(VaultUtils.EXTERNAL_REF_SHARED_SECRET_RECIPIENT, ss.toByteArray());
            }
        }

        if (contentLength != null) {
            builder = builder.addLong(VaultUtils.EXTERNAL_REF_CONTENT_LENGTH, contentLength);
        }
        return builder;
    }

    public static Certificate unwrapSignedExternalReference(CertificateReader transactionReader, int index) {
        Certificate signedExtRef = Certificate.fromByteArray(transactionReader.get(EXTERNAL_REF_SIGNED, index).asByteArray());
        CertificateReader reader = new CertificateReader(new CertificateParser(signedExtRef));
        try {
            return Certificate.fromByteArray(reader.getFirst(EXTERNAL_REF).asByteArray());
        } catch (MissingFieldException ex) {
            throw new IllegalArgumentException("Signed External Reference certificate is missing EXTERNAL_REF field. Found " + reader.getFields(), ex);
        }
    }

    //TODO: VELO_RESERVED_00B4 is reserved field. We need to work out if this aligns with the strategy, if so let's get it reserved for this purpose
    /**
     * A field that a transaction will use to hold external reference a signed by the holder of the data to verify the data is stored.
     */
    public static final int EXTERNAL_REF_SIGNED = Field.VELO_RESERVED_00B4;

    /**
     * A byte array field that a transaction will use to hold an external reference.
     */
    public static final int EXTERNAL_REF = Field.VELO_RESERVED_00B5;

    /**
     * A UUID field that identifies the type of external reference. This may signal a different certificate schema or storage mechanism.
     */
    public static final int EXTERNAL_REF_TYPE = Field.VELO_RESERVED_00B6;

    /**
     * A UUID field that provided a unique identifier for an external reference.
     */
    public static final int EXTERNAL_REF_ID =  Field.VELO_RESERVED_00B7;

    /**
     * A String field that identifies the media (MIME) type of the referenced content, as defined in RFC 2045 and 2046.
     */
    public static final int EXTERNAL_REF_CONTENT_MEDIA_TYPE = Field.VELO_RESERVED_00B8;

    /**
     * A long field that identifies the length of the referenced content in 8-bit bytes.
     */
    public static final int EXTERNAL_REF_CONTENT_LENGTH = Field.VELO_RESERVED_00B9;

    //TODO: implement handling of this
    /**
     * A byte array field for the encrypted shared secret for each recipient of the external reference (as per encrypted fields), signed by the encrypting user
     */
    public static final int EXTERNAL_REF_SHARED_SECRET_RECIPIENT =  Field.VELO_RESERVED_00BA;

    /**
     *  A byte array field that holds a signed the encrypted shared secret, signed by the encrypting user. One field for each recipient.
     */
    public static final int EXTERNAL_REF_SIGNATURE =  Field.VELO_RESERVED_00BB;

    /**
     * An integer field that holds the id of another field from the artifact which the data will be referenced by.
     */
    public static final int EXTERNAL_REF_ANCHOR_FIELD_ID = Field.VELO_RESERVED_00BC;

    /**
     * A String field that holds a file name which may have be used for the file before it was held as an external reference. It is just meta-data and is not used to track or store the file.
     */
    public static final int EXTERNAL_REF_ORIG_FILE_NAME =  Field.VELO_RESERVED_00BD;

}
