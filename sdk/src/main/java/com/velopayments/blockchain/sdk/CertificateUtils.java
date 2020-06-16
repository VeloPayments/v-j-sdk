package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.cert.CertificateReader;
import com.velopayments.blockchain.cert.Field;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Provides utilities for accessing {@code CertificateReader} fields, giving null when fields are absent instead
 * of throwing MissingFieldException
 */
public final class CertificateUtils {

    private CertificateUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Boolean getBoolean(CertificateReader reader, int fieldId) {
        return reader.getFields().contains(fieldId) ? (reader.getFirst(fieldId).asInt()==1) : null;
    }

    public static String getString(CertificateReader reader, int fieldId){
        return reader.getFields().contains(fieldId) ? reader.getFirst(fieldId).asString() : null;
    }

    public static UUID getUUID(CertificateReader reader, int fieldId){
        return reader.getFields().contains(fieldId) ? reader.getFirst(fieldId).asUUID() : null;
    }

    public static String getUUIDasString(CertificateReader reader, int fieldId){
        return reader.getFields().contains(fieldId) ? reader.getFirst(fieldId).asUUID().toString() : null;
    }

    public static Integer getInt(CertificateReader reader, int fieldId){
        return reader.getFields().contains(fieldId) ? reader.getFirst(fieldId).asInt() : null;
    }

    public static Long getLong(CertificateReader reader, int fieldId) {
        return reader.getFields().contains(fieldId) ? reader.getFirst(fieldId).asLong() : null;
    }

    public static Short getShort(CertificateReader reader, int fieldId) {
        return reader.getFields().contains(fieldId) ? reader.getFirst(fieldId).asShort() : null;
    }

    public static ZonedDateTime getZonedDateTime(CertificateReader reader, int fieldId) {
        if (reader.getFields().contains(fieldId)) {
            long dateTimeAsLong = reader.getFirst(fieldId).asLong();
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateTimeAsLong), ZoneId.of("UTC"));
        } else {
            return null;
        }
    }

    public static BigDecimal getBigDecimal(CertificateReader reader, int fieldId) {
        return reader.getFields().contains(fieldId) ? new BigDecimal(reader.getFirst(fieldId).asString()) : null;
    }

    public static byte[] getByteArray(CertificateReader reader, int fieldId){
        return reader.getFields().contains(fieldId) ? reader.getFirst(fieldId).asByteArray() : null;
    }

    @Deprecated
    public static List<CertificateReader> readTransactions(CertificateReader blockReader) {
        List<CertificateReader> transactions = new ArrayList<>();
        for (int i = 0; i < blockReader.count(Field.WRAPPED_TRANSACTION_TUPLE); ++i) {
            CertificateReader certReader = BlockchainUtils.createCertReader(
                blockReader.get(Field.WRAPPED_TRANSACTION_TUPLE, i).asByteArray());
            transactions.add(certReader);
        }
        return transactions;
    }
}
