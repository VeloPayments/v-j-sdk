package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.cert.*;
import com.velopayments.blockchain.crypt.EncryptionKeyPair;
import com.velopayments.blockchain.sdk.metadata.CoreMetadata;
import lombok.Builder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class BlockchainUtils {

    public static final UUID INITIAL_TRANSACTION_UUID = new UUID(0, 0);

    public static final String CERTIFICATE_MEDIA_TYPE = "application/vnd.velopayments.bc.certificate";

    private BlockchainUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    @Builder(builderMethodName = "encryptedTransactionCertificateBuilder", buildMethodName = "withFields")
    private static EncryptedCertificateBuilder encryptedTransactionCertificateBuilderBuilder(EncryptionKeyPair encryptionKeyPair,
                                                                                             UUID transactionId,
                                                                                             UUID previousTransactionId,
                                                                                             UUID transactionType,
                                                                                             UUID artifactId,
                                                                                             UUID artifactType,
                                                                                             Integer previousState,
                                                                                             Integer newState,
                                                                                             Long timestamp) {
        EncryptedCertificateBuilder builder = EncryptedCertificateBuilder.createCertificateBuilder(
            requireNonNull(encryptionKeyPair, "encryption key pair is required"),
            transactionType);
            //CertificateType.TRANSACTION);
        initTransactionBuilder(builder, transactionId, previousTransactionId, transactionType, artifactId, artifactType, previousState, newState, timestamp);
        return builder;
    }

    @Builder(builderMethodName = "transactionCertificateBuilder", buildMethodName = "withFields")
    private static CertificateBuilder transactionCertificateBuilderBuilder(UUID transactionId,
                                                                           UUID previousTransactionId,
                                                                           UUID transactionType,
                                                                           UUID artifactId,
                                                                           UUID artifactType,
                                                                           Integer previousState,
                                                                           Integer newState,
                                                                           Long timestamp) {
        CertificateBuilder builder = CertificateBuilder.createCertificateBuilder(CertificateType.TRANSACTION);
        initTransactionBuilder(builder, transactionId, previousTransactionId, transactionType, artifactId, artifactType, previousState, newState, timestamp);
        return builder;
    }

    private static void initTransactionBuilder(CertificateBuilder builder,
                                               UUID transactionId,
                                               UUID previousTransactionId,
                                               UUID transactionType,
                                               UUID artifactId,
                                               UUID artifactType,
                                               Integer previousState,
                                               Integer newState,
                                               Long timestamp) {
        builder.addUUID(Field.CERTIFICATE_ID, requireNonNull(transactionId, "transaction id is required"))
            .addUUID(Field.PREVIOUS_CERTIFICATE_ID, Optional.ofNullable(previousTransactionId).orElse(INITIAL_TRANSACTION_UUID))
            .addUUID(Field.TRANSACTION_TYPE, requireNonNull(transactionType, "transaction type is required"))
            .addUUID(Field.ARTIFACT_ID, requireNonNull(artifactId, "artifact id is required"))
            .addUUID(Field.ARTIFACT_TYPE, requireNonNull(artifactType, "artifact type is required"))
            .addInt(Field.PREVIOUS_ARTIFACT_STATE, Optional.ofNullable(previousState).orElse(CoreMetadata.VOID_STATE.getValue()))
            .addInt(Field.NEW_ARTIFACT_STATE, Optional.ofNullable(newState).orElse(CoreMetadata.CREATED_STATE.getValue()))
            .addLong(Field.CERTIFICATE_VALID_FROM, Optional.ofNullable(timestamp).orElse(Instant.now().toEpochMilli()));
    }

    @Deprecated
    public static CertificateReader createCertReader(Certificate cert) {
        return new CertificateReader(new CertificateParser(cert));
    }

    @Deprecated
    public static CertificateReader createCertReader(byte[] cert) {
        return createCertReader(Certificate.fromByteArray(cert));
    }
}
