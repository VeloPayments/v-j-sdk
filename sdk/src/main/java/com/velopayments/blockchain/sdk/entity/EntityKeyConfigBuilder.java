package com.velopayments.blockchain.sdk.entity;

import com.velopayments.blockchain.cert.*;
import com.velopayments.blockchain.crypt.*;
import com.velopayments.blockchain.sdk.ArrayUtils;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.metadata.ArtifactState;
import com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder;
import com.velopayments.blockchain.sdk.metadata.CoreMetadata;
import com.velopayments.blockchain.sdk.metadata.TransactionType;
import lombok.val;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static com.velopayments.blockchain.sdk.Base64Util.fromBase64;
import static com.velopayments.blockchain.sdk.Base64Util.toBase64;
import static com.velopayments.blockchain.sdk.BlockchainUtils.INITIAL_TRANSACTION_UUID;
import static com.velopayments.blockchain.sdk.entity.EntityKeyConfigContentType.UnprotectedPlusPassphraseProtected;
import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;
import static java.util.UUID.randomUUID;

/**
 * Helper class for building {@link EntityKeyConfig}s
 */
public final class EntityKeyConfigBuilder {

    private EntityKeyConfigBuilder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static EntityKeyConfig toEntityKeyConfig(UUID entityId, String entityName, EncryptionKeyPair encryptionKeyPair, SigningKeyPair signingKeyPair, String passPhrase) {
        val keyConfigBuilder = EntityKeyConfig.builder()
            .entityId(entityId)
            .entityName(entityName)
            .signingKeyPair(signingKeyPair)
            .encryptionKeyPair(encryptionKeyPair);

        if (passPhrase == null) {
            keyConfigBuilder.contentType(EntityKeyConfigContentType.UnprotectedOnly);
        }
        else {
            keyConfigBuilder.contentType(UnprotectedPlusPassphraseProtected);
            keyConfigBuilder.passphraseProtectedCertificateBase64(toCertificate(entityId, entityName, encryptionKeyPair, signingKeyPair, passPhrase));
        }
        return keyConfigBuilder.build();
    }

    public static final class EntityKeyConfigMetadata {
        public static final String ENTITY_KEY_CONFIG_TYPE_NAME = "ENTITY_KEY_CONFIG";
        public static final UUID ENTITY_KEY_CONFIG_TYPE_ID = UUID.fromString("98129efd-86e9-406d-80aa-e0d3ee7042a0");

        public static final TransactionType PRIVATE_ENTITY = new TransactionType(CertificateType.PRIVATE_ENTITY, "PRIVATE_ENTITY");

        public static final ArtifactState STATE_NEW = new ArtifactState(100, "NEW");
        public static final ArtifactState STATE_CREATED = new ArtifactState(101, "CREATED");

        private EntityKeyConfigMetadata() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static ArtifactTypeMetadataBuilder create() {
            return extractMetadata(ENTITY_KEY_CONFIG_TYPE_ID, ENTITY_KEY_CONFIG_TYPE_NAME, EntityKeyConfigMetadata.class);
        }
    }

    private static String toCertificate(UUID entityId, String entityName, EncryptionKeyPair encryptionKeyPair, SigningKeyPair signingKeyPair, String passPhrase) {
        val cert = BlockchainUtils.transactionCertificateBuilder()
            .transactionId(randomUUID())
            .previousTransactionId(INITIAL_TRANSACTION_UUID)
            .transactionType(EntityKeyConfigMetadata.PRIVATE_ENTITY.getId())
            .artifactId(entityId)
            .artifactType(EntityKeyConfigMetadata.ENTITY_KEY_CONFIG_TYPE_ID)
            .previousState(EntityKeyConfigMetadata.STATE_NEW.getValue())
            .newState(EntityKeyConfigMetadata.STATE_CREATED.getValue())
            .withFields()
            .addByteArray(Field.PUBLIC_ENCRYPTION_KEY, encryptionKeyPair.getPublicKey().getRawBytes())
            .addByteArray(Field.PRIVATE_ENCRYPTION_KEY, encryptionKeyPair.getPrivateKey().getRawBytes())
            .addByteArray(Field.PUBLIC_SIGNING_KEY, signingKeyPair.getPublicKey().getRawBytes())
            .addByteArray(Field.PRIVATE_SIGNING_KEY, signingKeyPair.getPrivateKey().getRawBytes())
            .addString(CoreMetadata.DISPLAY_NAME.getId(), entityName)
            .sign(entityId, signingKeyPair.getPrivateKey());

        byte[] saltPlusEncrypted = encrypt(cert.toByteArray(), passPhrase);
        return toBase64(saltPlusEncrypted);
    }

    /**
     * Rebuild an EntityKeyConfig from the persisted format
     */
    public static EntityKeyConfig fromJson(String json, String passPhrase) throws InvalidPassphraseException {
        val protectedKeyConfig = EntityKeysSerializer.fromJson(json);
        byte[] encryptedBytes = fromBase64(protectedKeyConfig.getPassphraseProtectedCertificateBase64());
        byte[] certSrc = decrypt(encryptedBytes, passPhrase);

        val reader = new CertificateReader(new CertificateParser(Certificate.fromByteArray(certSrc)));

        val encryptionKeyPair = new EncryptionKeyPair(
            new EncryptionPublicKey(reader.getFirst(Field.PUBLIC_ENCRYPTION_KEY).asByteArray()),
            new EncryptionPrivateKey(reader.getFirst(Field.PRIVATE_ENCRYPTION_KEY).asByteArray()));


        val signingKeyPair = new SigningKeyPair(
            new SigningPublicKey(reader.getFirst(Field.PUBLIC_SIGNING_KEY).asByteArray()),
            new SigningPrivateKey(reader.getFirst(Field.PRIVATE_SIGNING_KEY).asByteArray())
        );

        return EntityKeyConfig.builder()
            .entityId(reader.getFirst(Field.ARTIFACT_ID).asUUID())
            .entityName(reader.getFirst(CoreMetadata.DISPLAY_NAME.getId()).asString())
            .contentType(UnprotectedPlusPassphraseProtected)
            .encryptionKeyPair(encryptionKeyPair)
            .signingKeyPair(signingKeyPair)
            .passphraseProtectedCertificateBase64(protectedKeyConfig.getPassphraseProtectedCertificateBase64())
            .build();
    }

    /**
     * Encrypt the bytes using the pass phrase
     */
    static byte[] encrypt(byte[] plainBytes, String passPhrase) {
        Random r = new SecureRandom();
        byte[] salt = new byte[32];
        r.nextBytes(salt);

        Key key = Key.createFromPassword(salt, 10 * 1000, passPhrase);
        val cipher = new SimpleStreamCipher(key);
        byte[] encryptedBytes = cipher.encrypt(plainBytes);

        return ArrayUtils.addAll(salt, encryptedBytes);
    }

    /**
     * Decrypt the bytes using the pass phrase
     */
    static byte[] decrypt(byte[] encryptedBytes, String passPhrase) {
        try {
            byte[] salt = Arrays.copyOfRange(encryptedBytes, 0, 32);
            byte[] certEncrypted = Arrays.copyOfRange(encryptedBytes, 32, encryptedBytes.length);

            //use the pass phrase to access the certificate / reader
            Key key = Key.createFromPassword(salt, 10 * 1000, passPhrase);
            val cipher = new SimpleStreamCipher(key);
            return cipher.decrypt(certEncrypted);
        }
        catch (MessageAuthenticationException e) {
            throw new InvalidPassphraseException();
        }
    }
}


