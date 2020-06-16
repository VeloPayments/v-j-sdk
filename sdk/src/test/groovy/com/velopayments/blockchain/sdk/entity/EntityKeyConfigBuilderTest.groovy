package com.velopayments.blockchain.sdk.entity

import com.velopayments.blockchain.cert.*
import com.velopayments.blockchain.crypt.EncryptionKeyPair
import com.velopayments.blockchain.crypt.SigningKeyPair
import com.velopayments.blockchain.sdk.metadata.CoreMetadata
import org.junit.Test

import static com.velopayments.blockchain.sdk.Base64Util.fromBase64
import static groovy.test.GroovyAssert.shouldFail
import static java.util.UUID.randomUUID

class EntityKeyConfigBuilderTest {

    UUID entityId = randomUUID()
    String entityName = 'Bob Jones'
    EncryptionKeyPair encryptionKeyPair = EncryptionKeyPair.generate()
    SigningKeyPair signingKeyPair = SigningKeyPair.generate()
    String passphrase = 'ThePassword'


    @Test
    void 'test no passphrase'() {
        def keyconfig = EntityKeyConfigBuilder.toEntityKeyConfig(entityId, entityName, encryptionKeyPair, signingKeyPair, null)
        def json = EntityKeysSerializer.toJson(keyconfig)
        def roundTripped = EntityKeysSerializer.fromJson(json)
        assertKeyConfigContents(roundTripped)
    }


    @Test
    void 'test with passphrase round trip'() {
        def keyconfig = EntityKeyConfigBuilder.toEntityKeyConfig(entityId, entityName, encryptionKeyPair, signingKeyPair, passphrase)

        String json = EntityKeysSerializer.toJson(keyconfig)

        shouldFail(InvalidPassphraseException, {
            EntityKeyConfigBuilder.fromJson(json, 'ThisIsTheWrongPassphrase')
        })

        def roundTripped = EntityKeyConfigBuilder.fromJson(json, passphrase)
        assertKeyConfigContents(roundTripped)
    }


    @Test
    void 'test certificate details'() {
        def keyConfig = EntityKeyConfigBuilder.toEntityKeyConfig(entityId, entityName, encryptionKeyPair, signingKeyPair, passphrase)

        //assert what the certificate looks like
        byte[] encrypted = fromBase64(keyConfig.passphraseProtectedCertificateBase64)
        byte[] certSrc = EntityKeyConfigBuilder.decrypt(encrypted, passphrase)

        def reader = new CertificateReader(new CertificateParser(new Certificate(certSrc)))

        assert reader.getFirst(Field.ARTIFACT_ID).asUUID() == entityId
        assert reader.getFirst(Field.TRANSACTION_TYPE).asUUID() == CertificateType.PRIVATE_ENTITY
        assert reader.getFirst(CoreMetadata.DISPLAY_NAME.id).asString() == entityName
        assert reader.getFirst(Field.PUBLIC_ENCRYPTION_KEY).asByteArray() == encryptionKeyPair.publicKey.rawBytes
        assert reader.getFirst(Field.PRIVATE_ENCRYPTION_KEY).asByteArray() == encryptionKeyPair.privateKey.rawBytes
        assert reader.getFirst(Field.PUBLIC_SIGNING_KEY).asByteArray() == signingKeyPair.publicKey.rawBytes
        assert reader.getFirst(Field.PRIVATE_SIGNING_KEY).asByteArray() == signingKeyPair.privateKey.rawBytes
    }


        void assertKeyConfigContents(EntityKeyConfig keyconfig) {
        assert keyconfig.entityId == entityId
        assert keyconfig.entityName == entityName

        assert encryptionKeyPair.publicKey.rawBytes == keyconfig.encryptionKeyPair.publicKey.rawBytes
        assert encryptionKeyPair.privateKey.rawBytes == keyconfig.encryptionKeyPair.privateKey.rawBytes

        assert signingKeyPair.publicKey.rawBytes == keyconfig.signingKeyPair.publicKey.rawBytes
        assert signingKeyPair.privateKey.rawBytes == keyconfig.signingKeyPair.privateKey.rawBytes
    }


    @Test
    void 'test encryption on its own'() {
        def encrypted = EntityKeyConfigBuilder.encrypt("sandwich".bytes, "abracadabra")

        shouldFail(InvalidPassphraseException, {
            EntityKeyConfigBuilder.decrypt(encrypted, "wrong-passphrase")
        })

        assert "sandwich".bytes == EntityKeyConfigBuilder.decrypt(encrypted, "abracadabra")
    }

}
