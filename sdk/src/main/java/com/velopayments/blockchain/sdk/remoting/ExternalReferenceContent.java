package com.velopayments.blockchain.sdk.remoting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalReferenceContent {

    private UUID type;

    private boolean present;

    private UUID id;

    private String certificateBase64;

    private String contentType;

    private String signatureEncoded;

    private String originalFileName;

    private long contentLength;

    private String contentBytesBase64;
}
