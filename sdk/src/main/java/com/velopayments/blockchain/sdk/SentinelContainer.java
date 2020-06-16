package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.sentinel.*;
import com.velopayments.blockchain.sdk.sentinel.offsetstore.FileSystemOffsetStore;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class SentinelContainer {
    static final Pattern SENTINEL_PROPERY_NAME_PATTERN = Pattern.compile("sentinel\\.[\\w-_]+");
    static final String AGENT_HOST_PROPERTY = "agent.host";
    static final String AGENT_PORT_PROPERTY = "agent.port";
    static final String VAULT_PORT_PROPERTY = "vault.port";
    static final String AGENT_CONNNECTION_TO_PROPERTY = "agent.connection.timeout.millis";
    static final String AGENT_MAX_CONNNECTION_PROPERTY = "agent.max.connections";


    private final List<BlockCreationListener> blockCreationListeners = new Vector<>();  //Vector is thread-safe
    private final SentinelRegistry sentinelRegistry;
    private final BlockchainOperations blockchain;

    private final ScheduledExecutorService executor;
    private final SentinelPollerTask sentinelPollerTask;
    private ScheduledFuture<?> sentinelTaskHandle;
    private final long sentinelPollMillis;

    public SentinelContainer(BlockchainOperations blockchain) {
        this(blockchain, Executors.newScheduledThreadPool(1));
    }

    public SentinelContainer(BlockchainOperations blockchain, ScheduledExecutorService executor) {
        this.blockchain = requireNonNull(blockchain, "Blockchain is required");
        this.executor = requireNonNull(executor, "Executor is required");
        this.sentinelPollMillis = 1000;
        this.sentinelRegistry = new SentinelRegistry();
        var offsetStore = new FileSystemOffsetStore(Path.of(".").resolve("sentinel.log"));
        this.sentinelPollerTask = new SentinelPollerTask(blockchain, offsetStore, sentinelRegistry);
    }

    public void start() {
        sentinelRegistry.start();
        this.sentinelTaskHandle = this.executor.scheduleAtFixedRate(sentinelPollerTask::processLatestBlocks, sentinelPollMillis, sentinelPollMillis, MILLISECONDS);
    }

    public void stop() {
        if (this.sentinelTaskHandle != null) {
            this.sentinelTaskHandle.cancel(true);
        }
        this.executor.shutdown();
        log.debug("Closed");
    }

    /**
     * Register a {@code Sentinel} to handle transactions from the blockchain.
     * @param name a unique sentinel name to register
     * @param  properties property values which include properties to to use to create and configure the sentinel
     *
     * Note that this is for a one-time notification.
     * Any further sentinel notifications will be scheduled from the Criteria returned from the Sentinel notify method
     *
     * To cancel the sentinel subscription, use {@link RegistrationHandle#cancel()}
     */
    public RegistrationHandle register(String name, Properties properties) {
        String key = String.format("sentinel.%s", Objects.requireNonNull(name));
        String clazz = (String) properties.get(key);
        if (clazz == null) {
            throw new SentinelConfigException(key, null, "Sentinel class is required");
        }
        Sentinel sentinel = SentinelContainer.factory(name, clazz.trim());
        Map<String, Object> settings = sentinel.config().mapToSettings(name, properties);
        return sentinelRegistry.register(name, sentinel, settings, blockchain);
    }

    public void registerBlockCreationListener(BlockCreationListener listener) {
        this.blockCreationListeners.add(listener);
    }

    public void removeBlockCreationListener(BlockCreationListener listener) {
        this.blockCreationListeners.remove(listener);
    }

    public static void main(String[] args) throws Exception {
        Path configFile;
        if (args.length > 0) {
            configFile =  Path.of(args[0]);
        } else {
            configFile = Path.of("sentinels.properties");
        }
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(configFile));

        Path entityKeysPath;
        if (args.length > 1) {
            entityKeysPath =  Path.of(args[1]);
        } else {
            entityKeysPath = Path.of("sentinels.keys");
        }
        EntityKeys connectionKeys = EntityTool.fromJson(entityKeysPath);
        Duration timeout = null;
        if (properties.contains(AGENT_CONNNECTION_TO_PROPERTY))  {
            timeout = Duration.ofMillis(Long.parseLong(properties.getProperty(AGENT_CONNNECTION_TO_PROPERTY)));
        }
        Integer maxConnections = null;
        if (properties.contains(AGENT_MAX_CONNNECTION_PROPERTY)) {
            maxConnections = Integer.parseInt(properties.getProperty(AGENT_MAX_CONNNECTION_PROPERTY));
        }

        SentinelContainer sentinelContainer = null;
        try (RemoteBlockchain blockchain = RemoteBlockchain.builder()
                .entityKeys(connectionKeys)
                .agentHost(properties.getProperty(AGENT_HOST_PROPERTY, "localhost"))
                .agentPort(Integer.parseInt(properties.getProperty(AGENT_PORT_PROPERTY, String.valueOf(RemoteBlockchain.DEFAULT_AGENTD_PORT))))
                .vaultPort(Integer.parseInt(properties.getProperty(VAULT_PORT_PROPERTY, String.valueOf(RemoteBlockchain.DEFAULT_VAULT_PORT))))
                .agentConnectionTimeout(timeout)
                .maxAgentConnections(maxConnections)
                .build()) {
            blockchain.start();
            sentinelContainer = new SentinelContainer(blockchain);

            // configure sentinels
            for (String key : properties.stringPropertyNames()) {
                if (SENTINEL_PROPERY_NAME_PATTERN.matcher(key).matches()) {
                    sentinelContainer.register(key.substring(9), properties);
                }
            }
            log.info("Staring sentinels...");
            sentinelContainer.start();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            if (sentinelContainer != null) {
                sentinelContainer.stop();
            }
            log.info("Shutdown");
        }
    }

    private static Sentinel factory(String name, String className) {
        try {
            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(className);
            Object object = clazz.getDeclaredConstructor().newInstance();
            if (object instanceof Sentinel) {
                var sentinel = (Sentinel) object;
                log.info("Created \"{}\" sentinel: {}", name, clazz.getName());
                return sentinel;
            } else {
                throw new SentinelException("Class must implement Sentinel interface: " + className);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
            InstantiationException | InvocationTargetException  e) {
            throw new SentinelException("Cannot instantiate sentinel class: " + className, e);
        }
    }
}
