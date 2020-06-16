package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.cert.*;
import com.velopayments.blockchain.crypt.EncryptionPublicKey;
import com.velopayments.blockchain.crypt.SigningPublicKey;
import com.velopayments.blockchain.sdk.vault.VaultUtils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

import static com.velopayments.blockchain.cert.Field.*;

public class TransactionReader extends CertificateReaderSupport {

    public TransactionReader(Certificate cert) {
        super(cert);
    }

    public TransactionReader(CertificateReader certificateReader) {
        super(certificateReader);
    }

    public UUID getArtifactId() {
        return getUUID(ARTIFACT_ID);
    }

    public UUID getArtifactType() {
        return getUUID(ARTIFACT_TYPE);
    }

    public UUID getTransactionType() {
        return getUUID(TRANSACTION_TYPE);
    }

    public UUID getTransactionId() {
        return getUUID(CERTIFICATE_ID);
    }

    public UUID getPreviousTransactionId() {
        return certificateReader.getFirst(Field.PREVIOUS_CERTIFICATE_ID).asUUID();
    }

    public EncryptionPublicKey getPublicEncryptionKey() {
        if (certificateReader.getFields().contains(Field.PUBLIC_ENCRYPTION_KEY)) {
            return new EncryptionPublicKey(certificateReader.getFirst(Field.PUBLIC_ENCRYPTION_KEY).asByteArray());
        } else {
            return null;
        }
    }

    public SigningPublicKey getPublicSigningKey() {
        if (certificateReader.getFields().contains(Field.PUBLIC_SIGNING_KEY)) {
            return new SigningPublicKey(certificateReader.getFirst(Field.PUBLIC_SIGNING_KEY).asByteArray());
        } else {
            return null;
        }
    }

    public UUID getSignerId() {
        return getUUID(Field.SIGNER_ID);
    }

    public Integer getNewArtifactState() {
        return getInt(Field.NEW_ARTIFACT_STATE);
    }

    public Integer getPreviousArtifactState() {
        return getInt(Field.PREVIOUS_ARTIFACT_STATE);
    }

    public List<Certificate> getExternalReferences() {
        int count = certificateReader.count(VaultUtils.EXTERNAL_REF_SIGNED);
        List<Certificate> exrefs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            exrefs.add(getExternalReference(i));
        }
        return exrefs;
    }

    public Certificate getExternalReference(int index) {
        return VaultUtils.unwrapSignedExternalReference(certificateReader, index);
    }

    /**
     * @return a Map of CertificateReader instances, keyed by the id of the entity shared with, for each encrypted key.
     */
    public Map<UUID,CertificateReader> getEncryptedSharedSecrets() {
        Map<UUID,CertificateReader> certs = new HashMap<>();
        for (int i=0 ; i < countEncryptedSharedSecrets() ; i++) {
            byte[] fragment = this.certificateReader.get(Field.VELO_ENCRYPTED_SHARED_SECRET_FRAGMENT, i).asByteArray();
            CertificateReader r = new CertificateReader(new CertificateParser(Certificate.fromByteArray(fragment)));
            UUID entityId = r.getFirst(Field.VELO_ENCRYPTED_SHARED_SECRET_ENTITY_UUID).asUUID();
            certs.put(entityId, r);
        }
        return certs;
    }

    public int countEncryptedSharedSecrets() {
        return this.certificateReader.count(Field.VELO_ENCRYPTED_SHARED_SECRET_FRAGMENT);
    }
}
