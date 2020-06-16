package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.*;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import lombok.Builder;
import lombok.Singular;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public final class MutableFields {

    public static final UUID MUTABLE_FIELDS_CERT_TYPE_ID = UUID.fromString("04248ef5-6503-4aa8-866c-ae0fe40319fd");

    private MutableFields() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates a CertificateBuilder which is initialized for defining an ExternalReference to a mutable fields Certificate fragment. Example:
     * <pre>{@code
     *  Certificate mutableFieldsCertificate = CertificateBuilder.createCertificateFragmentBuilder()
     *      .addString(emailFieldId, "foo@example.com")
     *      .addInt(secretCodeFieldId, 1334434393)
     *      .sign(signer, signerKey);
     *  byte[] mutableFields = mutableFieldsCert.toByteArray();
     *
     *  Certificate externalReference = MutableFields.externalReferenceBuilder()
     *      .artifactId(artifactId)
     *      .referenceId(UUID.randomUUID())
     *      .schemaFrom(mutableFieldsCertificate)
     *      .contentLength((long) mutableFields.length)
     *      .signature(createSignature(mutableFields))p
     *      .sharedSecret(ssCertBackoffice)
     *      .sharedSecret(ssCertUser)
     *      .sharedSecret(ssCertAuditor)
     *      .withFields()
     *      .sign(signer, signerKey);
     *
     *  CompletableFuture<Certificate> signedRef = template.addExternalReference(externalReference, new ByteArrayInputStream(mutableFields))
     * }</pre>
     *
     * @param referenceId a unique id to use reference the fragment in the Vault
     * @param artifactId the id of an artifact to associate the mutable fields to
     * @param schemaFrom the mutable fields certificate fragment which will be used to define a schema by example
     * @param signature the content signature of the of the mutable fields fragment
     * @param contentLength the content length of the of the mutable fields fragment
     * @param sharedSecrets a collection of Certificates to define an encrypted shared secret for each recipient, signed by the encrypting user (as per encrypted fields)
     * @return an initialized CertificateBuilder
     */
    @Builder(builderMethodName = "externalReferenceBuilder", buildMethodName = "withFields")
    private static CertificateBuilder create(UUID referenceId,
                                             UUID artifactId,
                                             Certificate schemaFrom,
                                             byte[] signature,
                                             Long contentLength,
                                             @Singular Collection<Certificate> sharedSecrets) {
        byte[] copiedSignature = Arrays.copyOf(Objects.requireNonNull(signature, "Signature required"), signature.length);
        CertificateBuilder builder = CertificateBuilder.createCertificateFragmentBuilder()
            .addUUID(VaultUtils.EXTERNAL_REF_TYPE, MUTABLE_FIELDS_CERT_TYPE_ID)
            .addUUID(Field.ARTIFACT_ID, Objects.requireNonNull(artifactId, "Artifact id required"))
            .addUUID(VaultUtils.EXTERNAL_REF_ID, Objects.requireNonNull(referenceId, "Reference id required"))
            .addByteArray(VaultUtils.EXTERNAL_REF_SIGNATURE, copiedSignature)
            .addString(VaultUtils.EXTERNAL_REF_CONTENT_MEDIA_TYPE, BlockchainUtils.CERTIFICATE_MEDIA_TYPE);

        CertificateReader reader = new CertificateReader(new CertificateParser(Objects.requireNonNull(schemaFrom, "Schema-from is required")));
        for (Integer integer : reader.getFields()) {
            builder = builder.addInt(VaultUtils.EXTERNAL_REF_ANCHOR_FIELD_ID, integer);
        }

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
}
