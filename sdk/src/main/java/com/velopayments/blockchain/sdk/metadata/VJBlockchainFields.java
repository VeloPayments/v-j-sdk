package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.cert.Field;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class VJBlockchainFields {

    private VJBlockchainFields() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static Map<Integer, FieldMetadata> getStandardFieldMetadata() {
        Map<Integer, FieldMetadata> fieldMetadata = new HashMap<>();

        getStandardFieldNames().forEach((fieldId, fieldName) -> {
            final FieldType fieldType;
            boolean hidden = false;
            int sortOrder = 5000;
            //FieldValueDecoder fieldValueDecoder = null;
            switch (fieldId) {
                case Field.ARTIFACT_TYPE: fieldType = FieldType.UUID; /*fieldValueDecoder = artifactTypeDecoder ; */ sortOrder = 2005 ; break;
                case Field.ARTIFACT_ID: fieldType = FieldType.UUID; hidden = false ; sortOrder = 2010 ; break;
                case Field.TRANSACTION_TYPE: fieldType = FieldType.UUID; sortOrder = 2015 ; /*fieldValueDecoder = transactionTypeDecoder; */ break;
                case Field.CERTIFICATE_ID: fieldType = FieldType.UUID; sortOrder = 2020 ; break;
                case Field.SIGNER_ID: fieldType = FieldType.UUID; /*fieldValueDecoder = entityIdToEntityNameDecoder ; */ sortOrder = 2030 ; break;
                case Field.PREVIOUS_CERTIFICATE_ID: fieldType = FieldType.UUID; sortOrder = 2040 ; break;
                case Field.PUBLIC_ENCRYPTION_KEY: fieldType = FieldType.ByteArray; sortOrder = 3010 ; break;
                case Field.PUBLIC_SIGNING_KEY: fieldType = FieldType.ByteArray; sortOrder = 3020 ; break;
                case Field.PREVIOUS_ARTIFACT_STATE: fieldType = FieldType.Integer; /*fieldValueDecoder = stateValueToStateNameDecoder; */ sortOrder = 4000 ; break;
                case Field.NEW_ARTIFACT_STATE: fieldType = FieldType.Integer; /* fieldValueDecoder = stateValueToStateNameDecoder; */ sortOrder = 4010 ; break;
                case Field.CERTIFICATE_VALID_FROM: fieldType = FieldType.Date; hidden = true ; sortOrder = 4020 ; break;
                case Field.CERTIFICATE_VERSION: fieldType = FieldType.Integer; hidden = true ; sortOrder = 4030 ; break;
                case Field.CERTIFICATE_CRYPTO_SUITE: fieldType = FieldType.Unknown; hidden = true ; sortOrder = 4040 ; break;
                case Field.CERTIFICATE_TYPE: fieldType = FieldType.Unknown; hidden = true ; sortOrder = 4050 ; break;
                case Field.VELO_ENCRYPTED_SHARED_SECRET_FRAGMENT: fieldType = FieldType.Unknown; hidden = true ; sortOrder = 4060 ; break;
                case Field.RESERVED_ZERO_TAG: fieldType = FieldType.Unknown; hidden = true ; sortOrder = 4070 ; break;
                case Field.SIGNATURE: fieldType = FieldType.ByteArray; hidden = false ; sortOrder = 4080 ; break;
                default:
                    fieldType = FieldType.Unknown;
            }
            fieldMetadata.put(fieldId, new FieldMetadata(fieldId, fieldName, fieldType, false, hidden, sortOrder));
        });
        return fieldMetadata;
    }

    private static Map<Integer, String> getStandardFieldNames() {return Collections.unmodifiableMap(
        Stream.of(Field.class.getDeclaredFields())
            .filter(f ->
                !f.getName().startsWith("VELO_RESERVED_") &&
                    !f.getName().equals("RESERVED_ZERO_TAG") &&
                    !f.getName().startsWith("$")
            )
            .collect(Collectors.toMap(
                VJBlockchainFields::findFieldConstant,
                java.lang.reflect.Field::getName
            )));
    }

    private static Integer findFieldConstant(java.lang.reflect.Field field) {
        try {
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            //throw new RuntimeException("This will never happen!", e);
            return -1;
        }
    }
}
