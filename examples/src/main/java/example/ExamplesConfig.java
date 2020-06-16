package example;

import com.velopayments.blockchain.sdk.RemoteBlockchain;
import com.velopayments.blockchain.sdk.entity.EntityTool;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Set up a filesystem for the examples by setting up a directory for blockchain data and entity keys used
 * in the examples.
 * <p>
 * By default, the blockchain data directory will be created as a "data" subdirectory in the operating
 * directory. You can specify a different path by setting a value for example.path } property e.g.
 * <code>-Dexample.path=/etc/path/velochain/data</code>
 * <p>
 * By default, the entity keys directory will be created as a "entities" subdirectory in the operating
 * directory. You can specify a different path by setting a value for example.path } property e.g.
 * <code>-Dexample.keys=/etc/path/velochain/entities</code>
 */
public class ExamplesConfig {

    public static final String HOST_PROPERTY_NAME = "example.host";
    public static final String AGENT_PORT_PROPERTY_NAME = "example.port";
    public static final String VAULT_PORT_PROPERTY_NAME = "example.vaultPort";
    public static final String KEYS_PROPERTY_NAME = "example.keys";

    static final String DEFAULT_ENTITY_KEYS_DIR = "entities";
    static final String SERVICE_ENTITY_NAME = "service";
    static final String KEYCONFIG_EXT = ".keyconfig";

    private static class SingletonHolder {
        private static final ExamplesConfig INSTANCE = new ExamplesConfig();
    }

    public static ExamplesConfig getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private ExamplesConfig() {
        init();
    }

    /**
     * Load the entity keys from the filesystem. The default location is <code>./entities</code>, but it can be changed
     * to a path set with <code>-Dexample.keys=my_entity_keys</code>
     * @return a non-null java.nio.file.Path
     * @throws RuntimeException if the keys cannot be loaded.
     */
    public Path getEntityKeys() {
        String keys = Optional.ofNullable(System.getProperty(KEYS_PROPERTY_NAME)).orElse(DEFAULT_ENTITY_KEYS_DIR);
        return FileSystems.getDefault().getPath(keys);
    }

    public String getHost() {
        return Optional.ofNullable(System.getProperty(HOST_PROPERTY_NAME)).orElse("localhost");
    }

    public Integer getAgentPort() {
        return Optional.ofNullable(System.getProperty(AGENT_PORT_PROPERTY_NAME))
            .map(Integer::parseInt)
            .orElse(RemoteBlockchain.DEFAULT_AGENTD_PORT);
    }

    public Integer getVaultPort() {
        return Optional.ofNullable(System.getProperty(VAULT_PORT_PROPERTY_NAME))
            .map(Integer::parseInt)
            .orElse(RemoteBlockchain.DEFAULT_VAULT_PORT);
    }

    public Path getServiceEntityKeysConfigFile() {
        return getEntityKeysConfigFile(SERVICE_ENTITY_NAME, true);
    }

    /**
     * Gives a Path to the entity keys config file for the entity with the given name.
     * @param entityName the name of the entity
     * @param createIfAbsent true if an new entity key config file should be generated if the indicated one is absent
     * @return the Path to file
     */
    public Path getEntityKeysConfigFile(String entityName, boolean createIfAbsent) {
        Path keyConfig = getEntityKeys().resolve(entityName.toLowerCase() + KEYCONFIG_EXT);
        synchronized (SingletonHolder.INSTANCE) {
            if (createIfAbsent && !Files.exists(keyConfig)) {
                try {
                    Files.write(keyConfig, EntityTool.generateAsJson(entityName).getBytes());
                } catch (IOException e) {
                    throw new RuntimeException("Cannot create entity keys file at " + keyConfig, e);
                }
            }
        }
        return keyConfig;
    }

    private void init() {
        try {
            Files.createDirectories(getEntityKeys());
        } catch (IOException ex) {
            throw new RuntimeException("Cannot create keys dir: " + getEntityKeys(), ex);
        }
    }
}
