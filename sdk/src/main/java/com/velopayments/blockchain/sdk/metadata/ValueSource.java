package com.velopayments.blockchain.sdk.metadata;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.velopayments.blockchain.cert.CertificateReader;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FieldValueSource.class, name = "FIELD"),
    @JsonSubTypes.Type(value = StaticValueSource.class, name = "STATIC")
})
public interface ValueSource {

    String getValue(CertificateReader reader) throws CalculatedValueNotAvailableException;

}
