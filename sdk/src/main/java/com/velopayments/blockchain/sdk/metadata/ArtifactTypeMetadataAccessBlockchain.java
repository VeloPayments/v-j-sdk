package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.Signer;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.entity.EntityMetadata;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeHistory.*;
import static com.velopayments.blockchain.sdk.metadata.CoreMetadata.CORE_METADATA_TYPE_ID;
import static java.util.stream.Collectors.toList;

/**
 * This acts very much like a Java classloader but it loads {@link ArtifactTypeMetadata}s
 */
public class ArtifactTypeMetadataAccessBlockchain implements ArtifactTypeMetadataAccess {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ArtifactTypeMetadataAccessBlockchain.class);

    private final BlockchainOperations blockchain;
    private final Map<UUID, ArtifactTypeMetadata> cache = new HashMap<>();      //TODO we need to make this cache a singleton, because people create multiple instances of ArtifactTypeMetadataAccessBlockchain

    public ArtifactTypeMetadataAccessBlockchain(BlockchainOperations blockchain) {
        this.blockchain = Objects.requireNonNull(blockchain);
    }

    /**
     * Store {@link ArtifactTypeMetadata} in the blockchain
     * @return CompletableFuture holding the transaction result
     */
    @Override
    public CompletableFuture<TransactionStatus> store(ArtifactTypeMetadataBuilder builder, Signer signer) {
        return ensureCoreMetadataIsUpToDate(signer)
            .thenCompose(result -> {
                    if (result == TransactionStatus.SUCCEEDED) {
                        final UUID prevArtHistoryTransactionId = findPreviousMetadataTransactionId(ARTIFACT_TYPE_HISTORY_ARTIFACT_ID);
                        return ensureUpToDate(builder, UUID.randomUUID(), prevArtHistoryTransactionId, signer);
                    } else {
                        return CompletableFuture.completedFuture(result);
                    }
                });
    }

    private Certificate createArtifactTypeHistoryCert(ArtifactTypeMetadata metadata, UUID transactionId,  UUID prevTransactionId, Signer signer) {
        //UUID prevArtHistoryTransactionId = findPreviousMetadataTransactionId(ARTIFACT_TYPE_HISTORY_ARTIFACT_ID);
        //UUID transactionId = randomUUID();
        log.debug("Building ARTIFACT_TYPE_HISTORY transaction cert for {} {}->{}", metadata.getArtifactTypeName(), transactionId, prevTransactionId);
        return BlockchainUtils.transactionCertificateBuilder()
            .transactionId(transactionId)
            .previousTransactionId(prevTransactionId)
            .transactionType(TYPE_STORED.getId())
            .artifactId(ARTIFACT_TYPE_HISTORY_ARTIFACT_ID)
            .artifactType(ARTIFACT_TYPE_HISTORY_TYPE_ID)
            .withFields()
            .addUUID(ARTIFACT_TYPE_ID.getId(), metadata.getArtifactTypeId())
            .addString(CoreMetadata.DISPLAY_NAME.getId(), metadata.getArtifactTypeName() + " type history")
            .sign(signer.getEntityId(), signer.getSigningKeyPair().getPrivateKey());
    }

    /**
     * Find {@link ArtifactTypeMetadata} by the artifactTypeId
     * @return ArtifactTypeMetadata
     */
    @Override
    public Optional<ArtifactTypeMetadata> findByArtifactTypeId(UUID artifactTypeId) {
        ArtifactTypeMetadata cachedMetadata = readFromCache(artifactTypeId);
        if (cachedMetadata != null) {
            return Optional.of(cachedMetadata);
        }

        Optional<UUID> transactionId = blockchain.findLastTransactionIdForArtifactById(artifactTypeId);
        Optional<TransactionReader> lastTransaction = transactionId.flatMap(blockchain::findTransactionById);
        return lastTransaction.map(txnReader -> {
                ArtifactTypeMetadata metadata = ArtifactTypeMetadata.fromCertificate(txnReader, this);
                linkTypeHierarchyMetadata(artifactTypeId, metadata);
                return writeToCache(artifactTypeId, metadata);
        });
    }


    /**
     * Make sure that the type has a superType (parent type) unless it's CoreMetadata.
     * Think of CoreMetadata as being like java.lang.Object
     */
    private void linkTypeHierarchyMetadata(UUID artifactTypeId, ArtifactTypeMetadata metadata) {
        if (!CORE_METADATA_TYPE_ID.equals(artifactTypeId)) {    //prevent recursion through findByArtifactTypeId if a subtype is loaded before the core metadata
            ArtifactTypeMetadata coreMetadata = findByArtifactTypeId(CORE_METADATA_TYPE_ID)
                .orElseThrow(() -> new IllegalStateException("Did not find core metadata - please ensure that you have called ArtifactTypeMetadataAccessBlockchain.ensureCoreMetadataIsUpToDate on startup"));
            metadata.setParentMetadata(coreMetadata);
        }
    }

    private ArtifactTypeMetadata writeToCache(UUID artifactTypeId, ArtifactTypeMetadata metadata) {
        cache.put(artifactTypeId, metadata);
        return metadata;
    }

    private ArtifactTypeMetadata readFromCache(UUID artifactTypeId) {
        return cache.get(artifactTypeId);
    }


    /**
     * Ensure that an up-to-date copy of the SDK core metadata is stored on the blockchain
     *
     * @return CompletableFuture holding the transaction result
     */
     public CompletableFuture<TransactionStatus> ensureCoreMetadataIsUpToDate(Signer signer) {
        //ensure that the core types and the type type are persisted in the blockchain
         final UUID prevArtHistoryTransactionId = findPreviousMetadataTransactionId(ARTIFACT_TYPE_HISTORY_ARTIFACT_ID);
         final UUID coreHistoryTransactionId = UUID.randomUUID();
         final UUID artHistoryTransactionId = UUID.randomUUID();
         final UUID artTypeHistoryTransactionId = UUID.randomUUID();
         final UUID entityTypeHistoryTransActionId = UUID.randomUUID();
         return ensureUpToDate(CoreMetadata.create(), coreHistoryTransactionId, prevArtHistoryTransactionId, signer)
             .thenCompose(result -> {
                 if (result == TransactionStatus.SUCCEEDED) {
                     return ensureUpToDate(ArtifactTypeHistory.create(), artHistoryTransactionId, coreHistoryTransactionId, signer);
                 } else {
                     return CompletableFuture.completedFuture(result);
                 }
             })
             .thenCompose(result -> {
                 if (result == TransactionStatus.SUCCEEDED) {
                     return ensureUpToDate(ArtifactTypeType.create(), artTypeHistoryTransactionId, artHistoryTransactionId, signer);
                 } else {
                     return CompletableFuture.completedFuture(result);
                 }
             })
             .thenCompose(result -> {
                 if (result == TransactionStatus.SUCCEEDED) {
                     return ensureUpToDate(EntityMetadata.create(), entityTypeHistoryTransActionId, artTypeHistoryTransactionId, signer);
                 } else {
                     return CompletableFuture.completedFuture(result);
                 }
             });
     }

    private CompletableFuture<TransactionStatus> ensureUpToDate(ArtifactTypeMetadataBuilder builder, UUID artHistoryTransactionId, UUID prevArtHistoryTransactionId, Signer signer) {
        String typeName = builder.getMetadata().getArtifactTypeName();

        //if there is no super-type then link it to the core types.  Kindof like how everything in Java inherits from java.lang.Object
        UUID metaDataArtifactTypeId = builder.getMetadata().getArtifactTypeId();
        if (builder.getMetadata().getParentMetadata() == null && !CORE_METADATA_TYPE_ID.equals(metaDataArtifactTypeId)) {
            ArtifactTypeMetadata coreMetadata = findByArtifactTypeId(CORE_METADATA_TYPE_ID)
                .orElseThrow(() -> new IllegalStateException("Did not find core metadata - please ensure that you have called ArtifactTypeMetadataAccessBlockchain.ensureCoreMetadataIsUpToDate on startup"));
            builder.withParent(coreMetadata);
        }

        Optional<ArtifactTypeMetadata> storedMetadata;
        try {
            storedMetadata = findByArtifactTypeId(metaDataArtifactTypeId);
        } catch (Exception e) {
            log.warn("The metadata for {} has been corrupted and will be re-written", metaDataArtifactTypeId,  e);
            storedMetadata = Optional.empty();
        }

        //compare the stored metadata to the up-to-date metadata and write it fresh if necessary
        if (storedMetadata.isPresent()) {
            //is the stored metadata up to date?  If not then write a fresh copy to the blockchain
            String storedJson = ArtifactTypeMetadataBuilder.toJson(storedMetadata.get());
            String generatedJson = ArtifactTypeMetadataBuilder.toJson(builder.getMetadata());

            if (generatedJson.equals(storedJson)) {
                log.trace("[{}] metadata is already up to date", typeName);
                return CompletableFuture.completedFuture(TransactionStatus.SUCCEEDED);
            }
        }

        //first find the previous transaction id for the type, if it exists
        ArtifactTypeMetadata metadata = builder.getMetadata();
        UUID artifactTypeId = metadata.getArtifactTypeId();

        UUID prevTransactionId = findPreviousMetadataTransactionId(metadata.getArtifactTypeId());
        Certificate artifactMetadataCert = builder.build(prevTransactionId).sign(signer.getEntityId(), signer.getSigningKeyPair().getPrivateKey());
        linkTypeHierarchyMetadata(artifactTypeId, metadata);

        Certificate artifactTypeHistoryCert = createArtifactTypeHistoryCert(metadata, artHistoryTransactionId, prevArtHistoryTransactionId, signer);

        writeToCache(artifactTypeId, metadata); // FIXME: write to cache after canonization

        return blockchain.submit(artifactTypeHistoryCert)
            .thenCompose(result -> {
                if (result == TransactionStatus.SUCCEEDED) {
                    return blockchain.submit(artifactMetadataCert);
                } else {
                    return CompletableFuture.completedFuture(result);
                }
            });
    }

    private UUID findPreviousMetadataTransactionId(UUID artifactId) {
        Optional<UUID> ltr = blockchain.findLastTransactionIdForArtifactById(artifactId);
        return ltr.orElse(BlockchainUtils.INITIAL_TRANSACTION_UUID);
    }

    Map<UUID,ArtifactTypeMetadata> getCache() {
        return this.cache;
    }

    @Override
    public Collection<ArtifactTypeMetadata> listArtifactTypes() {
        //TODO use vjblockchain metadata API, when it is created
        Set<UUID> artifactTypeIds = new TreeSet<>();
        Optional<UUID> transactionId = blockchain.findLastTransactionIdForArtifactById(ARTIFACT_TYPE_HISTORY_ARTIFACT_ID);
        while(transactionId.isPresent()) {
            Optional<TransactionReader> transactionReader = transactionId.flatMap(blockchain::findTransactionById);
            transactionReader.ifPresent(transaction -> {
                UUID artTypeId = transaction.getFirst(ARTIFACT_TYPE_ID.getId()).asUUID();
                artifactTypeIds.add(artTypeId);
            });
            transactionId = transactionReader.map(TransactionReader::getPreviousTransactionId)
                .filter(i -> !BlockchainUtils.INITIAL_TRANSACTION_UUID.equals(i));
        }

        return artifactTypeIds.stream()
            .map(this::findByArtifactTypeId)
            .map(Optional::get)
            .sorted(Comparator.comparing(ArtifactTypeMetadata::getArtifactTypeName)) //sort by type name
            .collect(toList());
    }

    private static TransactionStatus bothSucceed(TransactionStatus s1, TransactionStatus s2) {
        return s1 != TransactionStatus.SUCCEEDED ? s1 : s2;
    }
}
