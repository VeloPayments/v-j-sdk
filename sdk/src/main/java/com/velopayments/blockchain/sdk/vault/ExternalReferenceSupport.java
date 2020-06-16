package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.sdk.CertificateReaderSupport;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static com.velopayments.blockchain.cert.Field.ARTIFACT_ID;

public abstract class ExternalReferenceSupport extends CertificateReaderSupport implements ExternalReference {
    protected final int[] anchorFields;

    public ExternalReferenceSupport(Certificate cert) {
        super(cert);
        this.anchorFields = new int[count(VaultUtils.EXTERNAL_REF_ANCHOR_FIELD_ID)];
        for (int i = 0; i < anchorFields.length; i++) {
            anchorFields[i] = certificateReader.get(VaultUtils.EXTERNAL_REF_ANCHOR_FIELD_ID, i).asInt();
        }
    }

    /**
     * @see ExternalReference#getExternalReferenceId()
     */
    @Override
    public UUID getExternalReferenceId() {
        return getUUID(VaultUtils.EXTERNAL_REF_ID);
    }

    /**
     * @see ExternalReference#getArtifactId()
     */
    @Override
    public UUID getArtifactId() {
        return getUUID(ARTIFACT_ID);
    }

    /**
     * @see ExternalReference#hasExternalField(int) (int)
     */
    @Override
    public boolean hasExternalField(int fieldId) {
        return Arrays.binarySearch(this.anchorFields, fieldId) >= 0;
    }


    /**
     * @see ExternalReference#getSignatureEncoded()
     */
    @Override
    public String getSignatureEncoded() {
        return Base64.getEncoder().encodeToString(getSignature());
    }

    public byte[] getSignature() {
        return getByteArray(VaultUtils.EXTERNAL_REF_SIGNATURE);
    }

    /**
     * @see ExternalReference#getOriginalFileName()
     */
    @Override
    public Optional<String> getOriginalFileName() {
        if (certificateReader.count(VaultUtils.EXTERNAL_REF_ORIG_FILE_NAME) > 0) {
            return Optional.of(getString(VaultUtils.EXTERNAL_REF_ORIG_FILE_NAME));
        } else {
            return Optional.empty();
        }
    }

    /**
     * @see ExternalReference#getExternalReferenceTypeId() ()
     */
    @Override
    public UUID getExternalReferenceTypeId() {
        return getUUID(VaultUtils.EXTERNAL_REF_TYPE);
    }

    /**
     * @see ExternalReference#getContentType()
     */
    @Override
    public String getContentType() {
        return getString(VaultUtils.EXTERNAL_REF_CONTENT_MEDIA_TYPE);
    }

    /**
     * @see ExternalReference#getContentLength()
     */
    @Override
    public long getContentLength() {
        return getLong(VaultUtils.EXTERNAL_REF_CONTENT_LENGTH);
    }
}
