package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.CertificateParser;
import com.velopayments.blockchain.cert.CertificateReader;
import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.client.RemoteAgentConfiguration;
import com.velopayments.blockchain.client.RemoteAgentConnection;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.crypt.EncryptionPrivateKey;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.guard.GuardRegistry;
import com.velopayments.blockchain.sdk.guard.PreSubmitGuard;
import com.velopayments.blockchain.sdk.vault.ExternalReference;
import com.velopayments.blockchain.sdk.vault.RemoteVault;
import com.velopayments.blockchain.sdk.vault.Vault;
import com.velopayments.blockchain.sdk.vault.VaultUtils;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;


/**
 * An implementation of {@code BlockchainOperations} which uses a local filesystem to store and access blockchain and
 * vault data.
 * <p>
 * Normally an instance of {@code RemoteBlockchain} would be created using the builder pattern which will give default
 * configuration where possible. A constructor is also available if more flexibility is needed.
*/
@Slf4j
public class RemoteBlockchain implements BlockchainOperations, AutoCloseable {

    public static final int DEFAULT_AGENTD_PORT = 4931;
    public static final int DEFAULT_VAULT_PORT = 11001;
    private static final long TO_SECS = 10;

    /**
     * RemoteBlockchain builder factory method
     */
    @Builder
    private static RemoteBlockchain createBlockchain(String agentHost,
                                                     Integer agentPort,
                                                     String vaultHost,
                                                     Integer vaultPort,
                                                     Integer maxAgentConnections,
                                                     Duration agentConnectionTimeout,
                                                     EntityKeys entityKeys) {
        Objects.requireNonNull(entityKeys, "EntityKeys is required");
        Objects.requireNonNull(entityKeys.getEntityId(), "EntityKeys has no entity id");
        Objects.requireNonNull(entityKeys.getEncryptionKeyPair(), "EntityKeys hos no encryption key pair");
        Objects.requireNonNull(entityKeys.getSigningKeyPair(), "EntityKeys hos no signing key pair");

        //FIXME: hard-coded for now
        UUID agentId = UUID.fromString("cb6c02aa-605f-4f81-bb01-5bb6f5975746");
        UUID entityId = UUID.fromString("aca029b6-2602-4b20-a8a4-cd8a95985a9a");
        EncryptionPrivateKey entityPrivateKey = new EncryptionPrivateKey(new byte[]{
            (byte) 0x77, (byte) 0x07, (byte) 0x6d, (byte) 0x0a, (byte) 0x73, (byte) 0x18, (byte) 0xa5, (byte) 0x7d,
            (byte) 0x3c, (byte) 0x16, (byte) 0xc1, (byte) 0x72, (byte) 0x51, (byte) 0xb2, (byte) 0x66, (byte) 0x45,
            (byte) 0xdf, (byte) 0x4c, (byte) 0x2f, (byte) 0x87, (byte) 0xeb, (byte) 0xc0, (byte) 0x99, (byte) 0x2a,
            (byte) 0xb1, (byte) 0x77, (byte) 0xfb, (byte) 0xa5, (byte) 0x1d, (byte) 0xb9, (byte) 0x2c, (byte) 0x2a});

        //UUID agentId = entityKeys.getAgentId();
        //UUID entityId = entityKeys.getEntityId();
        //EncryptionPrivateKey entityPrivateKey = entityKeys.getEncryptionKeyPair().getPrivateKey();

        var agentConfig = new RemoteAgentConfiguration(
            agentHost == null ? "localhost" : agentHost,
            agentPort == null ? DEFAULT_AGENTD_PORT : agentPort,
            agentId,
            null);

        // TODO: http comms with vault will likely need to be more configurable
        URI vaultUri;
        try {
            vaultUri = new URI("http", null,
                vaultHost == null ? agentConfig.getHost() : vaultHost, // default to the same host as agentd
                vaultPort == null ? DEFAULT_VAULT_PORT : vaultPort,
                null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid vault hostname: " + vaultHost, e);
        }

        Duration connTimeout = agentConnectionTimeout == null ? Duration.ofSeconds(10) : agentConnectionTimeout;
        int maxConnections = maxAgentConnections == null ? 16 : maxAgentConnections;
        return new RemoteBlockchain(
            new RemoteAgentConnectionPool(agentConfig, entityId, entityPrivateKey, maxConnections, connTimeout),
            new RemoteVault(HttpClient.newHttpClient(), vaultUri),
            new GuardRegistry());
    }

    private final Vault vault;
    private final GuardRegistry guardRegistry;

    private final RemoteAgentConnectionPool connectionPool;

    public RemoteBlockchain(RemoteAgentConnectionPool connectionPool, RemoteVault vault, GuardRegistry guardRegistry) {
        this.vault = Objects.requireNonNull(vault, "Vault is required");
        this.guardRegistry = Objects.requireNonNull(guardRegistry, "Guard Registry is required");
        this.connectionPool = Objects.requireNonNull(connectionPool, "Connection pool is required");
    }

    public void start() {
        try {
            this.connectionPool.initialize();
        } catch (IOException e) {
            throw new BlockchainIOException("Failure opening connection", e);
        }
    }

    /**
     * @see BlockchainOperations#close()
     */
    @Override
    public void close() {
        try {
            this.connectionPool.clear();
        } catch (IOException e) {
            throw new BlockchainIOException("Failure closing connection", e);
        }

        try {
            this.vault.close();
        } catch (RuntimeException e) {
            throw new BlockchainException("Failure closing vault", e);
        }
        log.debug("Closed");
    }

    /**
     * @see BlockchainOperations#submit(Certificate)
     */
    @Override
    public final CompletableFuture<TransactionStatus> submit(Certificate transactionCert) {
        guard(transactionCert);

        int length = transactionCert.toByteArray().length;
        RemoteAgentConnection conn = null;
        try {
            conn = connectionPool.borrowConnection();
            log.debug("Connection open for transaction ({} bytes)", length);
            return conn.submit(transactionCert);
        } catch (IOException e) {
            throw new BlockchainIOException(e);
        } finally {
            connectionPool.returnConnection(conn);
        }
    }

    private <T> CompletableFuture<T> requestWithAgentConnection(Function<RemoteAgentConnection, CompletableFuture<T>> unit) throws IOException {
        RemoteAgentConnection conn = null;
        try {
            conn = connectionPool.borrowConnection();
            return unit.apply(conn);
        } catch (Exception ex) {
            log.error("Request to blockchain failed", ex);
            throw ex;
        } finally {
            connectionPool.returnConnection(conn);
        }
    }


    protected RemoteAgentConnection getAgentConnection() {
        throw new UnsupportedOperationException("fix me later");
    }

    /**
     * @see BlockchainOperations#addExternalReference(Certificate, InputStream)
     */
    @Override
    public CompletableFuture<Certificate> addExternalReference(Certificate externalReference, InputStream inputStream) {
        CertificateReader reader = new CertificateReader(new CertificateParser(externalReference));
        // verify the artifact exists
        UUID artifactId = reader.getFirst(Field.ARTIFACT_ID).asUUID();
        findLastTransactionIdForArtifactById(artifactId)
            .orElseThrow(() -> new BlockchainException("External reference certificate identifies an artifact which was not found: " + artifactId));

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (log.isDebugEnabled()) log.debug("Store content to vault. [{}]", reader.get(VaultUtils.EXTERNAL_REF_CONTENT_LENGTH, 0).asLong());
                return vault.store(externalReference, inputStream);
                // TODO: validate written has size expected length?
            } catch (IOException e) {
                throw new BlockchainIOException("Failed to write Vault entry", e);
            }
        });
    }

    /**
     * @see BlockchainOperations#deleteExternalReference(Certificate)
     */
    @Override
    public boolean deleteExternalReference(Certificate externalReference) {
        try {
            log.debug("Delete vault content");
            return vault.delete(externalReference);
        } catch (IOException e) {
            UUID id = null;
            try {
                CertificateReader reader = new CertificateReader(new CertificateParser(externalReference));
                id = reader.getFirst(VaultUtils.EXTERNAL_REF_SIGNED).asUUID();
            } catch (Exception ex) {
                log.warn("Cannot read EXTERNAL_REF_ID field from ExternalReference certs", ex);
            }
            throw new BlockchainIOException("Failed to delete Vault entry: " + id, e);
        }
    }

    /**
     * @see BlockchainOperations#deleteExternalReferencesByArtifactId(UUID)
     */
    @Override
    public void deleteExternalReferencesByArtifactId(UUID artifactId) {
        Optional<TransactionReader> transaction = findLastTransactionIdForArtifactById(artifactId).flatMap(this::findTransactionById);
        do {
            transaction.ifPresent(reader -> reader.getExternalReferences().forEach(this::deleteExternalReference));
            transaction = transaction.flatMap(txn -> findTransactionById(txn.getPreviousTransactionId()));
        } while (transaction.isPresent());
    }

    /**
     * @see BlockchainOperations#loadExternalReferences(TransactionReader)
     */
    @Override
    public Stream<ExternalReference> loadExternalReferences(TransactionReader transaction) {
        return transaction.getExternalReferences().stream()
            .map(this::resolveExternalReference);
    }

    /**
     * @see BlockchainOperations#resolveExternalReference(Certificate)
     */
    @Override
    public ExternalReference resolveExternalReference(Certificate externalReferenceCert) {
        log.debug("Read vault content");
        return vault.get(externalReferenceCert);
    }

    /**
     * @see BlockchainOperations#findAllBlocksAfter(UUID)
     */
    @Override
    public Stream<BlockReader> findAllBlocksAfter(UUID afterBlockId) {
        Optional<UUID> nextBlockId = findNextBlockId(afterBlockId);
        return nextBlockId.flatMap(blockId -> findBlockById(blockId)
            .map(reader -> Stream.concat(Stream.of(reader), findAllBlocksAfter(blockId))))
            .orElse(Stream.empty());
    }

    /**
     * @see BlockchainOperations#findTransactionById(UUID)
     */
    @Override
    public Optional<TransactionReader> findTransactionById(UUID transactionId) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getTransactionById(transactionId);
                } catch (IOException  e) {
                    throw new BlockchainIOException("Failed to find transaction for " + transactionId, e);
                }
            })
            .get(TO_SECS, TimeUnit.SECONDS)
            .map(TransactionReader::new);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Failed to find transaction for " + transactionId, e);
        }
    }

    /**
     * @see BlockchainOperations#findNextTransactionIdForTransactionById(UUID)
     */
    @Override
    public Optional<UUID> findNextTransactionIdForTransactionById(UUID transactionId) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getNextTransactionIdForTransactionById(transactionId);
                } catch (IOException  e) {
                    throw new BlockchainIOException("Failed to find transaction for " + transactionId, e);
                }
            }).get(TO_SECS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Failed to find transaction for " + transactionId, e);
        }
    }

    /**
     * @see BlockchainOperations#findPreviousTransactionIdForTransactionById(UUID)
     */
    @Override
    public Optional<UUID> findPreviousTransactionIdForTransactionById(UUID transactionId) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getPreviousTransactionIdForTransactionById(transactionId);
                } catch (IOException  e) {
                    throw new BlockchainIOException("Failed to find transaction for " + transactionId, e);
                }
            }).get(TO_SECS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Failed to find transaction for " + transactionId, e);
        }
    }

    /**
     * @see BlockchainOperations#findFirstTransactionIdForArtifactById(UUID)
     */
    @Override
    public Optional<UUID> findFirstTransactionIdForArtifactById(UUID artifactId) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getFirstTransactionIdForArtifactById(artifactId);
                } catch (IOException  e) {
                    throw new BlockchainIOException("Failed to find first transaction id for artifact " + artifactId, e);
                }
            }).get(TO_SECS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Failed to find first transaction id for artifact " + artifactId, e);
        }
    }

    /**
     * @see BlockchainOperations#findLastTransactionIdForArtifactById(UUID)
     */
    @Override
    public Optional<UUID> findLastTransactionIdForArtifactById(UUID artifactId) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getLastTransactionIdForArtifactById(artifactId);
                } catch (IOException e) {
                    throw new BlockchainIOException("Failed to find last transaction id for artifact " + artifactId, e);
                }
            }).get(TO_SECS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Failed to find last transaction id for artifact " + artifactId, e);
        }
    }

    /**
     * @see BlockchainOperations#getLatestBlockId()
     */
    @Deprecated
    @Override
    // FIXME: cleanup this
    public Optional<UUID> findLastBlockIdForArtifactById(UUID artifactId) {
        throw new UnsupportedOperationException("No longer available");
    }

    @Override
    public UUID getLatestBlockId() {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getLatestBlockId();
                } catch (IOException  e) {
                    throw new BlockchainIOException("Could not read latest block id", e);
                }
            }).get(TO_SECS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Could not read latest block id", e);
        }
    }

    /**
     * @see BlockchainOperations#findNextBlockId(UUID)
     */
    @Override
    public Optional<UUID> findNextBlockId(UUID blockId) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getNextBlockId(blockId);
                } catch (IOException  e) {
                    throw new BlockchainIOException("Could not read next block after block " + blockId, e);
                }
            }).get(TO_SECS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Could not read next block after block " + blockId, e);
        }
    }

    /**
     * @see BlockchainOperations#findPrevBlockId(UUID)
     */
    @Override
    public Optional<UUID> findPrevBlockId(UUID blockId) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getPrevBlockId(blockId);
                } catch (IOException  e) {
                    throw new BlockchainIOException("Could not read next block after block " + blockId, e);
                }
            }).get(TO_SECS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Could not read previous block of block " + blockId, e);
        }
    }

    /**
     * @see BlockchainOperations#findBlockById(UUID)
     */
    @Override
    public Optional<UUID> findTransactionBlockId(UUID transactionId) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getTransactionBlockId(transactionId);
                } catch (IOException  e) {
                    throw new BlockchainIOException("Could not find block id for transaction id " + transactionId, e);
                }
            }).get(TO_SECS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Could not find block id for transaction id " + transactionId, e);
        }
    }


    protected void guard(Certificate transactionCert) {
        for (PreSubmitGuard guard : this.guardRegistry.getPreSumbitGuards()) {
            try {
                guard.evaluate(new TransactionReader(transactionCert), this);
            } catch (Exception ex) {
                log.warn("{} guard rejected transaction {}", guard.getClass().getName(), ex.getMessage());
                throw ex;
            }
        }
    }

    @Override
    public Optional<UUID> findBlockIdByBlockHeight(long blockHeight) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getBlockIdByBlockHeight(blockHeight);
                } catch (IOException  e) {
                    throw new BlockchainIOException("Could not find block id for block height " + blockHeight, e);
                }
            }).get(TO_SECS, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Could not find block id for block height " + blockHeight, e);
        }
    }

    @Override
    public Optional<BlockReader> findBlockById(UUID blockId) {
        try {
            return requestWithAgentConnection(connection -> {
                try {
                    return connection.getBlockById(blockId);
                } catch (IOException  e) {
                    throw new BlockchainIOException("Could not read block id" + blockId, e);
                }
            })
            .get(TO_SECS, TimeUnit.SECONDS)
            .map(BlockReader::new);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new BlockchainIOException("Could not read block id" + blockId, e);
        }
    }

    /**
     * @see BlockchainOperations#register(PreSubmitGuard)
     */
    @Override
    public void register(PreSubmitGuard guard) {
        this.guardRegistry.register(guard);
    }

    /**
     * @see BlockchainOperations#unregister(PreSubmitGuard)
     */
    @Override
    public void unregister(PreSubmitGuard guard) {
        this.guardRegistry.unregister(guard);
    }

    public Set<PreSubmitGuard> getPreSubmitGuards() {
        return this.guardRegistry.getPreSumbitGuards();
    }

    static final class RemoteAgentConnectionPool {
        private final BlockingQueue<RemoteAgentConnection> queue;
        private final RemoteAgentConfiguration agentConfig;
        private final UUID entityId;
        private final EncryptionPrivateKey entityPrivateKey;
        private final SocketFactory socketFactory;
        private Duration connectionTimeout;

        private boolean initialized;
        private AtomicInteger inUse = new AtomicInteger(0);
        private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        private ScheduledFuture<?> watchdogTask;


        public RemoteAgentConnectionPool(RemoteAgentConfiguration agentConfig, UUID entityId, EncryptionPrivateKey entityPrivateKey, int maxConnections,  Duration connectionTimeout) {
            this.agentConfig = agentConfig;
            this.entityId = entityId;
            this.entityPrivateKey = entityPrivateKey;
            this.socketFactory = SocketFactory.getDefault();

            this.queue = new LinkedBlockingQueue<>(maxConnections);
            // not worried about configuration yet
            this.connectionTimeout = connectionTimeout;
        }

        public void initialize() throws IOException {
            synchronized (this.queue) {
                log.info("Open {} Blockchain Agent connection(s) to {}:{}", queue.remainingCapacity(), agentConfig.getHost(), agentConfig.getPort());
                fillCapacity();
                this.watchdogTask = executorService.scheduleAtFixedRate(this::checkCapacity, 10, 1, TimeUnit.SECONDS);
                initialized = true;
            }
        }

        private void checkCapacity() {
            synchronized (this.queue) {
                int capacity = this.queue.remainingCapacity();
                if (this.inUse.get() < capacity) {
                    // Top up lost connections
                    log.info("Connection pool found to be below capacity {}, {} connections in use.", capacity, this.inUse.get());
                    try {
                        fillCapacity();
                    } catch (IOException e) {
                        log.warn("Unable to fill connection pool", e);
                    }
                }
            }
        }

        private void fillCapacity() throws IOException {
            while (true) {
                RemoteAgentConnection connection = new RemoteAgentConnection(this.agentConfig, socketFactory, this.entityId, this.entityPrivateKey);
                if (!queue.offer(connection)) {
                    break;
                }
                connection.connect();
                log.debug("Connection added");
            }
        }

        public void clear() throws IOException {
            synchronized (this.queue) {
                if (this.watchdogTask != null) {
                    this.watchdogTask.cancel(true);
                }
                executorService.shutdown();
                ArrayList<RemoteAgentConnection> connections = new ArrayList<>();
                int i = queue.drainTo(connections);
                connections.forEach(c -> {
                    if (c != null) {
                        try {
                            c.close();
                        } catch (Exception e) {
                            log.warn("IO error on closing connection", e);
                        }
                    }
                });
                log.debug("Closed {} connections", i);
                initialized = false;
            }
        }

        public RemoteAgentConnection borrowConnection() {
            //TODO: this is a somewhat simplistic pool. would likely be a good idea to scale up and down based on load
            if (!initialized) throw new BlockchainException("Connection pool not initialized");

            try {
                RemoteAgentConnection connection = queue.poll(connectionTimeout.toMillis(), TimeUnit.MILLISECONDS);
                if (connection == null) {
                    throw new BlockchainIOException("Connection pool depleted");
                }
                this.inUse.incrementAndGet();
                if (log.isDebugEnabled()) log.debug("Agent connection borrowed [{}]", queue.size());
                return connection;
            } catch (InterruptedException e) {
                throw new BlockchainIOException("Timeout waiting for available connection", e);
            }
        }

        public void returnConnection(RemoteAgentConnection conn) {
            if (conn != null) {
                Integer status;
                try {
                    status = conn.getConnectionStatus().get(500, TimeUnit.MILLISECONDS);
                } catch (CancellationException | InterruptedException ex) {
                    log.warn("Connection status check canceled");
                    status = 0; // probably a shutdown, ignore
                } catch (TimeoutException ex) {
                    log.warn("Connection status check not responding");
                    status = -10;
                } catch (IOException | ExecutionException ex) {
                    log.error("Connection status check error", ex);
                    status = -100;
                }

                if (status == 0) {
                    if (!queue.offer(conn)) {
                        log.info("Could not return connection. Pool is at capacity.[{}]. Closing connection", queue.size());
                        silentClose(conn);
                    } else {
                        this.inUse.decrementAndGet();
                    }
                    if (log.isDebugEnabled()) log.debug("Agent connection returned [{}]", queue.size());
                } else {
                    log.warn("Connection status check failed: {}. Replacing connection", status);
                    silentClose(conn);
                }
            }
        }

        private void silentClose(RemoteAgentConnection conn) {
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
                log.debug("Error closing connection",e);
            }
        }

    }
}
