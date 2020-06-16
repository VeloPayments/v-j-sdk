package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.CertificateParser;
import com.velopayments.blockchain.cert.CertificateReader;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;


public class MultipartEncoderTest {

    RemoteVault vault;

    EntityKeys keys = EntityTool.generate("test");
    byte[] content = "Some text to reference".getBytes();
    byte[] signature = ExternalReference.createMessageDigest().digest(content);
    UUID id = randomUUID();
    UUID artifactId = randomUUID();
    String contentType = "text/plain";
    String fileName = "foo.txt";

    @Test
    public void multipartEncoding() throws  Exception {
        Certificate certificate = VaultUtils.externalReferenceBuilder()
            .referenceId(id)
            .artifactId(artifactId)
            .anchorField(110)
            .contentType(contentType)
            .contentLength((long) content.length)
            .signature(signature)
            .withFields()
            .addString(VaultUtils.EXTERNAL_REF_ORIG_FILE_NAME, fileName)
            .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey());

        String boundry = MultipartEncoder.createMultiPartEncodingBoundtry();
        CertificateReader reader = new CertificateReader(new CertificateParser(certificate));
        ByteArrayInputStream content = new ByteArrayInputStream(this.content);
        HttpRequest.BodyPublisher bodyPublisher = MultipartEncoder.encode(reader, content, boundry);

        assertThat(new String(MultipartEncoder.encodeToBytes(reader, content, boundry),StandardCharsets.UTF_8))
            .isEqualTo(new String(MultipartEncoder.encodeToBytesRaw(reader, content, boundry),StandardCharsets.UTF_8));

        RemoteVaultTest.ResponseSubscriber subscriber = new RemoteVaultTest.ResponseSubscriber();
        bodyPublisher.subscribe(subscriber);
        byte[] bytes = subscriber.out.toByteArray();

        List<String> partHeaders = new String(bytes).lines()
            .limit(4)
            .collect(Collectors.toList());
        assertThat(partHeaders).hasSize(4);
        assertThat(partHeaders.get(0))
            .as("The boundary delimiter MUST occur at the beginning of a line")
            .isEqualTo("--" + boundry);
        assertThat(partHeaders.get(1))
            .isEqualTo("Content-Disposition: form-data; name=\"cert\"");
        assertThat(partHeaders.get(2))
            .isEqualTo("Content-Type: application/vnd.velopayments.bc.certificate");
        assertThat(partHeaders.get(3))
            .isEqualTo("Content-Transfer-Encoding: binary");


        //TODO : check all boundaries

    }
}
