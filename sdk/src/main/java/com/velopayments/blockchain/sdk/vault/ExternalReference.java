package com.velopayments.blockchain.sdk.vault;


import com.velopayments.blockchain.cert.Certificate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

/**
 * An interface for classes that can provide a reference from a blockchain transaction to a resource which is
 * external to the blockchain, such as a vault data.
 * <p>
 * An {@code ExternalReference} encapsulates a signed {@code Certificate} with metadata describing the externally
 * reference. The certificate is stored on the blockchain, strongly anchoring the external data. The external data
 * can deleted but not modified without invalidating the external reference {@code Certificate} on the blockchain.
 * <p>
 * The interface provides methods to read the external data. These methods verify the integrity of the data as it
 * is read, by checking the signature provided in the external reference {@code Certificate}. If the integrity is not
 * validated, reading will fail with an IO exception.
 * <p>
 * This is useful when the blockchain must reference data that is too large to be stored on a block certificate, such as
 * a file. Because externally referenced resources can be deleted, it is also useful for sensitive data which must not
 * be immutably stored on the blockchain, such as personally identifiable information.
 */
public interface ExternalReference {

    /**
     * Provides the external reference {@code Certificate} with fields describing the metadata of the referenced data.
     * @return a non-null {@code Certificate}
     */
    Certificate getCertificate();

    /**
     * A {@code UUID} that identifies the type of external reference. This may signal a different certificate schema or
     * storage mechanism.
     * <p>
     * (At this time, only a version 1 vault reference is supported).
     * @return a non-null type id
     * @see VaultUtils#VAULT_EXTERNAL_REF_TYPE_ID
     */
    UUID getExternalReferenceTypeId();

    /**
     * A {@code UUID} that identifies the externally referenced data
     * @return a non-null id
     */
    UUID getExternalReferenceId();

    /**
     * A {@code UUID} that identifies the artifact which references the data
     * @return a non-null id
     */
    UUID getArtifactId();

    /**
     * Indicates if the externally referenced data is present, meaning it has not been deleted.
     * @return true if present or false if it has been removed
     */
    boolean isPresent();

    /**
     * Indicates if the external reference {@code Certificate} has a field with the given id.
     * @param fieldId a non-null field id to search for
     * @return true if the field is exists, false if it does not
     */
    boolean hasExternalField(int fieldId);

    /**
     * Gives the content type (aka MIME type) of the data
     * @return a non-null {@code String} with the content type
     */
    String getContentType();

    /**
     * Gives the length of the data in terms of the number of bytes.
     * @return a {@code long} with the length
     */
    long getContentLength();

    /**
     * Gives the signature of the content as a Base64-encoded string.
     * @return a non-null {@code String} with the signature;
     */
    String getSignatureEncoded();

    /**
     * Gives a {@code String} with the original name of the file, if applicable.
     * @return an {@code Optional} with the name, or an empty {@code Optional} if it was not provided
     */
    Optional<String> getOriginalFileName();

    /**
     * Retrieve the externally referenced data, returning an input stream to read the data from.
     * <p>
     * The stream will not be buffered, and is not required to support the mark or reset methods. The stream is also not
     * required to be safe for access by concurrent threads. It's the responsibility of the caller to close the stream
     * once the data has been read.
     * <p>
     * The integrity of the data will be verified against the signature from the external reference {@code Certificate}.
     * If the integrity is not validated, reading will fail with IO exception.
     * @return a new input stream
     * @throws IOException if an I/O error occurs, or if the integrity of data is not verified
     */
    InputStream read() throws IOException;

    /**
     * Read in and return all the bytes from the externally referenced data.
     * <p>
     * Note that this method is intended for simple cases where it is convenient to read all bytes into a byte array. It
     * is not intended for reading in large files.
     * <p>
     * The integrity of the data will be verified against the signature from the external reference {@code Certificate}.
     * If the integrity is not validated, reading will fail with IO exception.
     * @return a byte array containing the bytes read
     * @throws IOException  if an I/O error occurs reading the data, or if the integrity of data is not verified
     */
    default byte[] asByteArray() throws IOException {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int n;
            byte[] buffer = new byte[1024 * 4];
            try(final InputStream input = read()) {
                while ((n = input.read(buffer)) != -1) {
                    output.write(buffer, 0, n);
                }
            }
            return output.toByteArray();
        }
    }


    /**
     * Retrieve the externally referenced data, returning a new NIO channel to read the data.
     * <p>
     * It's the responsibility of the caller to close the channel once the data has been read.
     * @return a new readable byte channel
     * <p>
     * The integrity of the data will be verified against the signature from the external reference {@code Certificate}.
     * If the integrity is not validated, reading will fail with IO exception.
     * @throws IOException if an I/O error occurs,  or if the integrity of data is not verified
     */
    default ReadableByteChannel readableChannel() throws IOException {
        return Channels.newChannel(read());
    }

    /**
     * (temporary)
     *
     * Creates a {@code MessageDigest} for use for as a "signature"
     * @return a {@code MessageDigest}
     */
    // TODO: MessageDigest will be replaced with digitial signatures for external ref signature
    static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Vault cannot create MessageDigest", e);
        }
    }
}
