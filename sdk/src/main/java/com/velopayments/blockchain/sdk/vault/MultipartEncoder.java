package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.CertificateReader;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import lombok.experimental.UtilityClass;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@UtilityClass
public class MultipartEncoder {

    private static final String PART_NAME_CERT = "cert";
    private static final String PART_NAME_FILE = "file";

    String createMultiPartEncodingBoundtry() {
        return "----" + UUID.randomUUID().toString();
    }

    /**
     * Encode multipart body with both external reference {@link CertificateReader} and an {@link InputStream}
     *
     * @param externalReference Certificate to encode
     * @param content {@link InputStream} to encode
     * @param boundary multipart boundary
     * @return {@link BodyPublisher} for {@link HttpRequest}
     *
     * @throws IOException {@link ByteArrayOutputStream} IO error
     */
    public BodyPublisher encode(CertificateReader externalReference, InputStream content, String boundary) throws IOException{
        byte[] bytes = encodeToBytes(externalReference, content, boundary);
        return HttpRequest.BodyPublishers.ofByteArray(bytes);
    }

    /**
     * Encode multipart body with external reference {@link CertificateReader}
     *
     * @param externalReference Certificate to encode
     * @param boundary multipart boundary
     * @return {@link BodyPublisher} for {@link HttpRequest}
     *
     * @throws IOException {@link ByteArrayOutputStream} IO error
     */
    public BodyPublisher encode(CertificateReader externalReference, String boundary) throws IOException{
        byte[] bytes = encodeToBytes(externalReference, boundary);
        return HttpRequest.BodyPublishers.ofByteArray(bytes);
    }

    @Deprecated // Manual encoding is discouraged for now as we could make all sorts of mistakes.
    public BodyPublisher encodeRaw(CertificateReader externalReference, InputStream content, String boundary) throws IOException{
        byte[] bytes = encodeToBytesRaw(externalReference, content, boundary);
        return HttpRequest.BodyPublishers.ofByteArray(bytes);
    }

    byte[] encodeToBytes(CertificateReader externalReference, InputStream content, String boundary) throws IOException {
        ContentType certificateContentType = ContentType.create(BlockchainUtils.CERTIFICATE_MEDIA_TYPE);
        ContentType attachmentContentType = ContentType.create(externalReference.getFirst(VaultUtils.EXTERNAL_REF_CONTENT_MEDIA_TYPE).asString());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        HttpEntity multipartEntity = MultipartEntityBuilder.create()
            .setBoundary(boundary)
            .addBinaryBody(PART_NAME_CERT, externalReference.getCertificate().toByteArray(), certificateContentType,null)
            // TODO: I punted and read the bytes into memory ... we should stream this
            .addBinaryBody(PART_NAME_FILE, content.readAllBytes(), attachmentContentType, null)
            .build();
        multipartEntity.writeTo(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    byte[] encodeToBytes(CertificateReader externalReference, String boundary) throws IOException {
        ContentType certificateContentType = ContentType.create(BlockchainUtils.CERTIFICATE_MEDIA_TYPE);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        HttpEntity multipartEntity = MultipartEntityBuilder.create()
            .setBoundary(boundary)
            .addBinaryBody(PART_NAME_CERT, externalReference.getCertificate().toByteArray(), certificateContentType,null)
            .build();
        multipartEntity.writeTo(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


    @Deprecated // Manual encoding is discouraged for now as we could make all sorts of mistakes.
    byte[] encodeToBytesRaw(CertificateReader externalReference, InputStream content, String boundary) throws IOException {
        // TODO: I punted and read the bytes into memory ... we should stream this
        byte[] separator = ("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(separator);
        out.writeBytes("Content-Disposition: form-data; name=\"cert\"\r\n".getBytes(StandardCharsets.UTF_8));

        String contentType = "Content-Type: " + BlockchainUtils.CERTIFICATE_MEDIA_TYPE + "\r\n\r\n";
        out.writeBytes("Content-Type: ".getBytes(StandardCharsets.UTF_8));
        out.writeBytes(BlockchainUtils.CERTIFICATE_MEDIA_TYPE.getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        out.writeBytes("Content-Transfer-Encoding: binary".getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.writeBytes(externalReference.getCertificate().toByteArray());
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        out.writeBytes(separator);

        out.writeBytes("Content-Disposition: form-data; name=\"file\"\r\n".getBytes(StandardCharsets.UTF_8));
        out.writeBytes("Content-Type: ".getBytes(StandardCharsets.UTF_8));
        out.writeBytes(externalReference.getFirst(VaultUtils.EXTERNAL_REF_CONTENT_MEDIA_TYPE).asString().getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        out.writeBytes("Content-Transfer-Encoding: binary".getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.writeBytes(content.readAllBytes());
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        out.writeBytes(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
