package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.CertificateParser;
import com.velopayments.blockchain.cert.CertificateReader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import static com.velopayments.blockchain.sdk.vault.VaultUtils.VAULT_EXTERNAL_REF_TYPE_ID;

/**
 * Vault implementation over REST
 * @see Vault
 */
@Slf4j
public class RemoteVault implements Vault {

    private final HttpClient client;
    private final URI baseUri;

    /**
     * Create a Vault and initialize the Crypto Filesystem.
     */
    public RemoteVault(URI vaultUri) {
        this(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build(), vaultUri);
    }

    public RemoteVault(HttpClient client, URI vaultUri) {
        this.client = client;
        this.baseUri = Objects.requireNonNull(vaultUri);
    }

    /**
     * @see Vault#close()
     */
    @Override
    public void close() {
    }

    /**
     * @see Vault#store(Certificate, InputStream)
     */
    @Override
    public Certificate store(Certificate externalReference, InputStream contentStream) throws IOException {
        if (contentStream == null) {
            throw new IllegalArgumentException("Cannot store null contentStream");
        }

        String boundary = MultipartEncoder.createMultiPartEncodingBoundtry();
        CertificateReader reader = new CertificateReader(new CertificateParser(externalReference));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(createUri("/v1/vault/store"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", multiPartMediaType(boundary))
            .POST(MultipartEncoder.encode(reader, contentStream, boundary))
            .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return Certificate.fromByteArray(response.body());
            } else {
                throw new VaultException("Failed to store resource: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            throw new VaultException("Vault request interrupted", e);
        } catch (IOException e) {
            throw new VaultException("Error reading response", e);
        }
    }

    /**
     * @see Vault#get(Certificate)
     */
    @Override
    public ExternalReference get(Certificate externalReference) {
        CertificateReader fragReader = new CertificateReader(new CertificateParser(externalReference));
        if  (fragReader.count(VaultUtils.EXTERNAL_REF_TYPE) == 0) {
            throw new IllegalArgumentException(String.format("External Reference certificate is missing a %s (%d) field. Found %s",
                String.format("EXTERNAL_REF_TYPE (%d)", VaultUtils.EXTERNAL_REF_TYPE), VaultUtils.EXTERNAL_REF_TYPE, fragReader.getFields()));
        }
        UUID refType = fragReader.getFirst(VaultUtils.EXTERNAL_REF_TYPE).asUUID();

        if (!VAULT_EXTERNAL_REF_TYPE_ID.equals(refType) && !MutableFields.MUTABLE_FIELDS_CERT_TYPE_ID.equals(refType)) {
            throw new IllegalArgumentException("Unsupported Vault external references type: " + refType);
        }

        return new ExternalReferenceRestClient(externalReference, this::requestResource);
    }

    /**
     * @see Vault#delete(Certificate)
     */
    @Override
    public boolean delete(Certificate externalReference) throws IOException {
        if (externalReference == null) {
            throw new IllegalArgumentException("Cannot store null contentStream");
        }
        String boundary = MultipartEncoder.createMultiPartEncodingBoundtry();
        CertificateReader reader = new CertificateReader(new CertificateParser(externalReference));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(createUri("/v1/vault/remove"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", multiPartMediaType(boundary))
            .POST(MultipartEncoder.encode(reader, boundary))
            .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return true;
            }
            else if (response.statusCode() == 404) {
                return false;
            } else {
                throw new VaultException("Cannot delete resource: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            throw new VaultException("Vault request interrupted", e);
        } catch (IOException e) {
            throw new VaultException("Error reading response", e);
        }
    }

    private URI createUri(String path) {
        try {
            return new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(), path, null, null);
        } catch (URISyntaxException e) {
            throw new VaultException("Invalid uri from: " + baseUri);
        }
    }

    private HttpResponse<InputStream> requestResource(CertificateReader reader) {
        String boundary = MultipartEncoder.createMultiPartEncodingBoundtry();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(createUri("/v1/vault/resolve"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", multiPartMediaType(boundary))
                .POST(MultipartEncoder.encode(reader, boundary))
                .build();
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            throw new VaultException("Vault request interrupted", e);
        } catch (IOException e) {
            throw new VaultException("Communication failure getting resource content", e);
        }
    }

    private static String multiPartMediaType(String boundary) {
        return "multipart/form-data;boundary=" + boundary;
    }
}
