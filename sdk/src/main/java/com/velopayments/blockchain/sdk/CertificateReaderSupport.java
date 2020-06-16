package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.cert.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.velopayments.blockchain.cert.Field.CERTIFICATE_CRYPTO_SUITE;

public abstract class CertificateReaderSupport {
    protected final CertificateReader certificateReader;

    public CertificateReaderSupport(Certificate cert) {
        this(new CertificateReader(new CertificateParser(cert)));
    }

    public CertificateReaderSupport(CertificateReader certificateReader) {
        this.certificateReader = certificateReader;
    }

    public Certificate getCertificate() {
        return certificateReader.getCertificate();
    }

    public CertificateReader getCertificateReader() {
        return this.certificateReader;
    }

    public Long getValidFrom() {
        return getLong(Field.CERTIFICATE_VALID_FROM);
    }

    public String getCryptoSuite() {
        return getString(CERTIFICATE_CRYPTO_SUITE);
    }

    public Integer getVersion() {
        return this.certificateReader.getFirst(Field.CERTIFICATE_VERSION).asInt();
    }

    public UUID getCertificateType() {
        return this.certificateReader.getFirst(Field.CERTIFICATE_TYPE).asUUID();
    }

    public CertificateFieldReader getFirst(int fieldId) {
        return this.certificateReader.getFirst(fieldId);
    }

    public Set<Integer> getFields() {
        return certificateReader.getFields();
    }

    public int count(int fieldId) {
        return certificateReader.count(fieldId);
    }

    public CertificateFieldReader get(int fieldId, int index) {
        return certificateReader.get(fieldId, index);
    }

    public Optional<CertificateFieldReader> findFirst(int fieldId) {
        if (this.certificateReader.getFields().contains(fieldId)) {
            return Optional.of(this.certificateReader.getFirst(Field.CERTIFICATE_VALID_FROM));
        } else {
            return Optional.empty();
        }
    }

    public String getString(int fieldId) {
        return CertificateUtils.getString(this.certificateReader, fieldId);
    }

    public UUID getUUID(int fieldId) {
        return CertificateUtils.getUUID(this.certificateReader, fieldId);
    }

    public Long getLong(int fieldId) {
        return CertificateUtils.getLong(this.certificateReader, fieldId);
    }

    public Integer getInt(int fieldId) {
        return CertificateUtils.getInt(this.certificateReader, fieldId);
    }

    public Boolean getBoolean(int fieldId) {
        return CertificateUtils.getBoolean(this.certificateReader, fieldId);
    }

    public byte[] getByteArray(int fieldId) {
        return CertificateUtils.getByteArray(this.certificateReader, fieldId);
    }

}
