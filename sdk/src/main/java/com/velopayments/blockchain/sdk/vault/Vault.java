package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.Certificate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * An interface to provide a vault for storing externally referenced data.
 *
 * @see ExternalReference
 */
public interface Vault {

    /**
     * Close the vault. A closed vault cannot be reopened.
     *
     * @throws VaultException if there was a problem closing the vault
     */
    void close();

    /**
     * Stores content in the vault. Creating the vault entry requires an external reference {@code Certificate} with
     * metadata describing the content, and the content to store.
     * <p>
     * The integrity of the data will be verified against the signature from the external reference {@code Certificate}.
     * If the integrity is not validated, writing will fail with IO exception.
     * <p>
     * A vault receipt {@code Certificate} will be returned, and should then be stored on the blockchain to strongly
     * anchor the external content. The vault receipt wraps the given external reference {@code Certificate}, and is
     * signed by a vault entity.
     * @param externalReference a non-null external reference certificate, with fields describing the given content
     * @param content the byte array with the data to store in the vault
     * @return a vault receipt certificate
     * @throws IOException if an I/O error occurs writing to or creating the vault entry
     * @throws IllegalArgumentException is the external reference certificate is invalid or does not verify the content
     * @throws VaultException is operation cannot be performed
     * @see VaultUtils
     */
    default Certificate store(Certificate externalReference, byte[] content) throws IOException {
        return store(externalReference, new ByteArrayInputStream(content));
    }


    /**
     * Stores content in the vault. Creating the vault entry requires an external reference {@code Certificate} with
     * metadata describing the content, and the content to store.
     * <p>
     * The integrity of the data will be verified against the signature from the external reference {@code Certificate}.
     * If the integrity is not validated, writing will fail with IO exception.
     * <p>
     * A vault receipt {@code Certificate} will be returned, and should then be stored on the blockchain to strongly
     * anchor the external content. The vault receipt wraps the given external reference {@code Certificate}, and is
     * signed by a vault entity.
     * @param externalReference a non-null external reference certificate, with fields describing the given content
     * @param content the input stream to read from for the data to store in the vault
     * @return a vault receipt certificate
     * @throws IOException if an I/O error occurs writing to or creating the vault entry
     * @throws IllegalArgumentException is the external reference certificate is invalid or does not verify the content
     * @throws VaultException is operation cannot be performed
     * @see VaultUtils
     */
    Certificate store(Certificate externalReference, InputStream content) throws IOException;

    /**
     * Get content from the vault for the given external reference {@code Certificate}. Returns an
     * {@code ExternalReference} object to allow for lazy retrieval of the content.
     * @param externalReference a non-null external reference certificate, with fields describing the given content
     * @return an external reference
     * @throws IllegalArgumentException is the given certificate is invalid
     * @throws VaultException is operation cannot be performed
     */
    ExternalReference get(Certificate externalReference);

    /**
     * Delete an entry from the vault if it exists.
     * @return  if true deleted, false if it didn't exist
     * @param externalReference an external reference certificate describing the resource to delete
     * @throws IOException if an I/O error occurs deleting the vault entry
     * @throws VaultException is operation cannot be performed
     */
    boolean delete(Certificate externalReference) throws IOException;
}
