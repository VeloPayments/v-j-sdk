package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.cert.CertificateReader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticValueSource implements ValueSource {

    private String staticValue;

    @Override
    public String getValue(CertificateReader reader) {
        return staticValue;
    }
}
