package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExternalReferenceRemoteTest {

    EntityKeys keys = EntityTool.generate("test");
    byte[] text = "Some text to reference".getBytes();
    byte[] signature = ExternalReference.createMessageDigest().digest(text);
    UUID id = randomUUID();
    UUID artifactId = randomUUID();
    String contentType = "text/plain";
    String fileName = "foo.txt";

    Certificate certificate = VaultUtils.externalReferenceBuilder()
        .referenceId(id)
        .artifactId(artifactId)
        .anchorField(110)
        .contentType(contentType)
        .contentLength((long) text.length)
        .signature(signature)
        .withFields()
        .addString(VaultUtils.EXTERNAL_REF_ORIG_FILE_NAME, fileName)
        .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey());

    @Test
    public void presentRefereence() throws Exception {
        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(new ByteArrayInputStream(text));
        ExternalReferenceRestClient extRef = new ExternalReferenceRestClient(certificate, cert -> mockResponse);

        assertThat(extRef.getExternalReferenceId()).isEqualTo(id);
        assertThat(extRef.getArtifactId()).isEqualTo(artifactId);
        assertThat(extRef.getContentLength()).isEqualTo(text.length);
        assertThat(extRef.getSignatureEncoded()).isEqualTo(Base64.getEncoder().encodeToString(signature));
        assertThat(extRef.getOriginalFileName()).hasValue(fileName);

        assertThat(extRef.isPresent()).isTrue();
        try (InputStream in = extRef.read()) {
            assertThat(in.readAllBytes()).isEqualTo(text);
        }
    }

    @Test(expected = IOException.class)
    public void deletedReference() throws Exception {
        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockResponse.body()).thenThrow(IOException.class);
        ExternalReferenceRestClient extRef = new ExternalReferenceRestClient(certificate, cert -> mockResponse);
        assertThat(extRef.isPresent()).isFalse();

        try (InputStream in = extRef.read()) {
            assertThat(in.readAllBytes()).isEqualTo(text);
        }
    }

    @Test(expected = VaultException.class)
    public void errorResponse() throws Exception {
        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn(new ByteArrayInputStream(text));
        ExternalReferenceRestClient extRef = new ExternalReferenceRestClient(certificate, cert -> mockResponse);

        assertThat(extRef.isPresent()).isFalse();
    }
}
