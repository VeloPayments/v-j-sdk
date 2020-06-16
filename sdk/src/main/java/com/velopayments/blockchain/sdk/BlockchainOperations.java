package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.guard.PreSubmitGuard;
import com.velopayments.blockchain.sdk.vault.ExternalReference;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Specifies the base set of Blockchain operations.
 */
public interface BlockchainOperations {

    /**
     * Submits a transaction to the blockchain. Submitting a transaction will not guarantee that it is canonized in the
     * blockchain unless the returned future completes with {@code TransactionStatus#SUCCEEDED}. Note that it can
     * take up to (and slightly longer than) the configured commit interval for submitted transactions to be committed.
     *
     * @param transaction  a non-null artifact transaction {@code Certificate} to submit
     * @return a {@code CompletableFuture} which will provide the {@code TransactionStatus} once the transaction is
     *       committed
     */
    CompletableFuture<TransactionStatus> submit(Certificate transaction);

    /**
     * Adds an external reference to the vault. The external reference certificate is best to construct with
     * {@code VaultUtils}, so that the certificate will provide the fields required by the vault.
     *
     * @param externalReference a non-null external reference {@code Certificate} describing the data to store
     * @param inputStream a non-null {@code InputStream} providing the data to store in the vault
     * @return a {@code CompletableFuture} which will provide a signed vault receipt {@code Certificate} once the
     *      file is sorted in the vault.
     */
    CompletableFuture<Certificate> addExternalReference(Certificate externalReference, InputStream inputStream);

    /**
     * Delete an external reference from the vault.
     *
     * @param externalReferenceCert a non-null external reference {@code Certificate} describing the data to remove
     * @return true if an object was removed from the vault, and false if object was not present in the vault.
     */
    // TODO: change this to use a vault receipt
    boolean deleteExternalReference(Certificate externalReferenceCert);

    /**
     * Remove all objects in the vault referenced by a given artifact.
     *
     * @param artifactId a non-null id of the artifact
     */
    void deleteExternalReferencesByArtifactId(UUID artifactId);

    /**
     * Load all external references named in the given transaction. The {@code ExternalReference} objects returned can
     * be used to lazily retrieve objects from the vault.
     *
     * @param reader a non-null transaction {@code TransactionReader}
     * @return a {@code Stream} of {@code ExternalReference} objects for each external reference made in the transaction
     */
    Stream<ExternalReference> loadExternalReferences(TransactionReader reader);

    /**
     * Given an external reference {@code Certificate}, lookup and return an {@code ExternalReference} which
     * can be used to lazily retrieve the object from the vault.
     * @param externalReference a non-null externalReference {@code Certificate}
     * @return an {@code ExternalReference}
     */
    ExternalReference resolveExternalReference(Certificate externalReference);

    /**
     * Gives the id of the most most latest block.
     * @return the id of the highest block
     */
    UUID getLatestBlockId();

    /**
     * Find the block id for a transaction
     * @return an {@code Optional} with the block id, or an empty {@code Optional} if not found
     */
    Optional<UUID> findTransactionBlockId(UUID transactionId);

    /**
     * Find the id of the block preceding the block with the given id, i.e. a block height of one less.
     * @param blockId  a non-null block id
     * @return an {@code Optional} with the block id, or an empty {@code Optional} if not found
     */
    Optional<UUID> findPrevBlockId(UUID blockId);

    /**
     * Find the id of the block next the block with the given id, i.e. a block height of one greater.
     * @param blockId  a non-null block id
     * @return an {@code Optional} with the block id, or an empty {@code Optional} if not found
     */
    Optional<UUID> findNextBlockId(UUID blockId);

    /**
     * Find the block {@code Certificate} for the block with the given id.
     * @param blockId  a non-null block id
     * @return an {@code Optional} with the block certificate reader, or an empty {@code Optional} if not found
     */
    Optional<BlockReader> findBlockById(UUID blockId);

    /**
     * Find the transaction {@code Certificate} for the transaction with the given id.
     * @param transactionId  a non-null transaction id
     * @return an {@code Optional} with the certificate, or an empty {@code Optional} if not found
     */
    Optional<TransactionReader> findTransactionById(UUID transactionId);

    /**
     * Find the id of next transaction following the transaction with the given id.
     * @param transactionId  a non-null transaction id
     * @return an {@code Optional} with the id, or an empty {@code Optional} if not found
     */
    Optional<UUID> findNextTransactionIdForTransactionById(UUID transactionId);

    /**
     * Find the id of the transaction preceding the transaction with the given id.
     * @param transactionId  a non-null transaction id
     * @return an {@code Optional} with the id, or an empty {@code Optional} if not found
     */
    Optional<UUID> findPreviousTransactionIdForTransactionById(UUID transactionId);

    /**
     * Find the id of the first (oldest) transaction for the artifact with the given id.
     * @param artifactId  a non-null artifact id
     * @return an {@code Optional} with the certificate, or an empty {@code Optional} if not found
     */
    Optional<UUID> findFirstTransactionIdForArtifactById(UUID artifactId);

    /**
     * Find the id of the last (newest) transaction for the artifact with the given id.
     * @param artifactId  a non-null artifact id
     * @return an {@code Optional} with the certificate, or an empty {@code Optional} if not found
     */
    Optional<UUID> findLastTransactionIdForArtifactById(UUID artifactId);

    /**
     * Finds the certificates for all blocks after the block with the given id.
     * @param targetBlock a non-null block id
     * @return a {@code Stream} of {@code CertificateReader} objects for each block after the target block
     */
    Stream<BlockReader> findAllBlocksAfter(UUID targetBlock);

    /**
     * Find the last block id for an artifact
     *
     * @param artifactId the artifact id
     *
     * @return an {@code Optional} with the block id, or an empty {@code Optional} if not found
     */
    Optional<UUID> findLastBlockIdForArtifactById(UUID artifactId);     //    waiting on BLOC-179

    /**
     * Register a {@code PreSubmitGuard} to handle transactions from the blockchain.
     * @param guard  a non-null {@code PreSubmitGuard}
     */
    void register(PreSubmitGuard guard);

    /**
     * Unregister a {@code PreSubmitGuard} so that it no longer receives transactions from the blockchain to handle.
     * @param guard  a non-null {@code PreSubmitGuard}
     */
    void unregister(PreSubmitGuard guard);

    /**
     * Close the blockchain
     */
    void close();

    Optional<UUID> findBlockIdByBlockHeight(long blockHeight);

}
