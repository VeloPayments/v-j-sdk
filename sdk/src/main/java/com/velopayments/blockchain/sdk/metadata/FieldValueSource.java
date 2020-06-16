package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.cert.CertificateReader;
import com.velopayments.blockchain.cert.FieldConversionException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValueSource implements ValueSource {

    private int fieldId;

    @Override
    public String getValue(CertificateReader reader) throws CalculatedValueNotAvailableException {
        if (!reader.getFields().contains(fieldId)) {
            throw new CalculatedValueNotAvailableException();
        }

        try {
            return reader.getFirst(fieldId).asString();
        }
        catch (FieldConversionException e) {
            //only supporting String field types at the moment because we don't have a use case for the others and we would have had to write tests for them all.
            return "Field " + fieldId + " was not of type: String";
        }
    }

}
