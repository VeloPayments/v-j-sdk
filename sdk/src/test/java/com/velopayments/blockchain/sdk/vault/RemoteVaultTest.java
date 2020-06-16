package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.CertificateBuilder;
import com.velopayments.blockchain.cert.CertificateParser;
import com.velopayments.blockchain.cert.CertificateReader;
import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.testcontainers.containers.MockServerContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class RemoteVaultTest {

    @ClassRule
    public static MockServerContainer vaultContainer = new MockServerContainer();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    RemoteVault vault;

    EntityKeys keys = EntityTool.generate("test");
    byte[] content = "Some text to reference".getBytes();
    byte[] signature = ExternalReference.createMessageDigest().digest(content);
    UUID id = randomUUID();
    UUID artifactId = randomUUID();
    String contentType = "text/plain";
    String fileName = "foo.txt";

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

    @Before
    public void setUp(){
        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create(vaultContainer.getEndpoint());
        vault = new RemoteVault(client, uri);
    }

    @Test
    public void store() throws Exception {
        mockClient()
            .when(request()
                .withMethod("POST")
                .withPath("/v1/vault/store")
                .withHeaders(
                    header("Content-Type", "multipart/form-data.*")),
                Times.exactly(1))
            .respond(response()
                .withStatusCode(200)
                .withHeaders(header("Content-Type", BlockchainUtils.CERTIFICATE_MEDIA_TYPE))
                .withBody(signedVaultReceipt(certificate)));

        Certificate receipt = vault.store(certificate, content);

        var reader = new CertificateReader(new CertificateParser(receipt));
        assertThat(receipt).isNotNull();
        assertThat(reader.getFirst(VaultUtils.EXTERNAL_REF).asByteArray()).isEqualTo(certificate.toByteArray());
    }

    @Test
    public void store_500_status() throws Exception {
        expectedException.expect(VaultException.class);
        expectedException.expectMessage("Failed to store resource: 500");

        mockClient()
            .when(request()
                .withMethod("POST")
                .withPath("/v1/vault/store")
                .withHeaders(
                    header("Content-Type", "multipart/form-data.*")),
                Times.exactly(1)) // only once so that it doesn't mess up other cases
            .respond(response().withStatusCode(500));

        vault.store(certificate, content);
    }

    @Test
    public void store_IO_Error() throws Exception {
        expectedException.expect(VaultException.class);
        expectedException.expectMessage("Error reading response");

        mockClient()
            .when(request()
                    .withMethod("POST")
                    .withPath("/v1/vault/store")
                    .withHeaders(
                        header("Content-Type", "multipart/form-data.*")),
                Times.exactly(1)) // only once so that it doesn't mess up other cases
            .error(error().withDropConnection(true));

        vault.store(certificate, content);
    }

    @Test
    public void resolve() throws Exception {
        mockClient()
            .when(request()
                .withMethod("POST")
                .withPath("/v1/vault/resolve")
                .withHeaders(
                    header("Content-Type", "multipart/form-data.*")))
            .respond(response()
                .withStatusCode(200)
                .withHeaders(header("Content-Type", contentType))
                .withBody(content));

        ExternalReference externalReference = vault.get(certificate);

        assertThat(externalReference).isNotNull();
        assertThat(externalReference.getCertificate()).isNotNull();
        assertThat(externalReference.getExternalReferenceId()).isEqualTo(id);
        assertThat(externalReference.getArtifactId()).isEqualTo(artifactId);
        assertThat(externalReference.getContentLength()).isEqualTo(content.length);
        assertThat(externalReference.getExternalReferenceTypeId()).isEqualTo(VaultUtils.VAULT_EXTERNAL_REF_TYPE_ID);
        assertThat(externalReference.getSignatureEncoded()).isEqualTo(Base64.getEncoder().encodeToString(signature));
        assertThat(externalReference.getContentType()).isEqualTo(contentType);
        assertThat(externalReference.getOriginalFileName()).hasValue(fileName);

        assertThat(externalReference.isPresent()).isTrue();
        assertThat(externalReference.read()).hasSameContentAs(new ByteArrayInputStream(content));

        // still available?
        assertThat(externalReference.isPresent()).isTrue();
        assertThat(externalReference.read()).hasSameContentAs(new ByteArrayInputStream(content));
    }

    @Test
    public void resolve_IOError() throws Exception {
        expectedException.expect(VaultException.class);
        expectedException.expectMessage("Communication failure getting resource content");

        mockClient()
            .when(request()
                .withMethod("POST")
                .withPath("/v1/vault/resolve")
                .withHeaders(
                    header("Content-Type", "multipart/form-data.*")),
                Times.exactly(1))
            .error(
                error().withDropConnection(true));

        ExternalReference externalReference = vault.get(certificate);
        assertThat(externalReference.isPresent()).isFalse();
    }


    @Test
    public void delete() throws Exception {
        mockClient()
            .when(request()
                .withMethod("POST")
                .withPath("/v1/vault/remove")
                .withHeader(header("Content-Type", "multipart/form-data.*")),
                Times.exactly(1))
            .respond(response()
                .withStatusCode(200)
                .withHeaders(header("Content-Type", contentType))
                .withBody(content));

        assertThat(vault.delete(certificate)).isTrue();
    }

    @Test
    public void deleteNotPresent() throws Exception {
        mockClient()
            .when(request()
                .withMethod("POST")
                .withPath("/v1/vault/delete")
                .withHeader(header("Content-Type", "multipart/form-data.*")),
                Times.exactly(1))
            .respond(response()
                .withHeaders(header("Content-Type", contentType))
                .withStatusCode(404));

        assertThat(vault.delete(certificate)).isFalse();
    }

    @Test
    public void delete_IOError() throws Exception {
        expectedException.expect(VaultException.class);
        expectedException.expectMessage("Error reading response");

        mockClient()
            .when(request()
                .withMethod("POST")
                .withPath("/v1/vault/remove")
                .withHeader(header("Content-Type", "multipart/form-data.*")),
                Times.exactly(1))
            .error(error().withDropConnection(true));

        assertThat(vault.delete(certificate)).isFalse();
    }

    @Test
    public void delete_500() throws Exception {
        expectedException.expect(VaultException.class);
        expectedException.expectMessage("Cannot delete resource: 500");

        mockClient()
            .when(request()
                .withMethod("POST")
                .withPath("/v1/vault/remove")
                .withHeader(header("Content-Type", "multipart/form-data.*")),
                Times.exactly(1))
            .respond(response().withStatusCode(500));

        assertThat(vault.delete(certificate)).isFalse();
    }


    private byte[] signedVaultReceipt(Certificate externalReference) {
        return CertificateBuilder.createCertificateFragmentBuilder()
            .addByteArray(VaultUtils.EXTERNAL_REF, externalReference.toByteArray())
            .addLong(Field.CERTIFICATE_VALID_FROM, ZonedDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli())
            .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey())
            .toByteArray();
    }

    private MockServerClient mockClient() {
        return new MockServerClient(vaultContainer.getContainerIpAddress(), vaultContainer.getServerPort());
    }

    // adapt Flow.Subscriber<List<ByteBuffer>> to Flow.Subscriber<ByteBuffer>
    static final class StringSubscriber implements Flow.Subscriber<ByteBuffer> {
        final java.net.http.HttpResponse.BodySubscriber<String> wrapped;
        StringSubscriber(java.net.http.HttpResponse.BodySubscriber<String> wrapped) {
            this.wrapped = wrapped;
        }
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            wrapped.onSubscribe(subscription);
        }
        @Override
        public void onNext(ByteBuffer item) { wrapped.onNext(List.of(item)); }
        @Override
        public void onError(Throwable throwable) { wrapped.onError(throwable); }
        @Override
        public void onComplete() { wrapped.onComplete(); }
    }

    static class ResponseSubscriber implements Flow.Subscriber<ByteBuffer> {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @SneakyThrows
        @Override
        public void onNext(ByteBuffer item) {
            out.write(item.array());
        }

        @Override
        public void onError(Throwable t) {
            Assertions.fail("onError", t);
        }

        @Override
        public void onComplete() {
        }
    };
}
