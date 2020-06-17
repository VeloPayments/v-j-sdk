package example.sentinels;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.*;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataAccess;
import com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataAccessBlockchain;
import com.velopayments.blockchain.sdk.metadata.TransactionType;
import example.ExamplesConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static example.sentinels.CustomerMetadata.*;
import static java.util.UUID.randomUUID;

/**
 * Simple demonstration of:
 *
 * (1) creating a blockchain with a local connection
 * (2) add a sentinel that will send an email welcome message
 * (3) create a transaction that will trigger the sentinel
 * (4) submit the transaction
 */
public class SentinelExample {

    public static void main(String[] args) throws Exception {
        ExamplesConfig config = ExamplesConfig.getInstance();
        EntityKeys connectionKeys = EntityTool.fromJson(config.getServiceEntityKeysConfigFile());

        final EntityKeys bobKeys = EntityTool.fromJson(ExamplesConfig.getInstance().getEntityKeysConfigFile("bob", true));

        Thread thread = sentinelContainerThread(config);

        try (RemoteBlockchain blockchain = RemoteBlockchain.builder()
            .entityKeys(connectionKeys)
            .agentHost(config.getHost())
            .agentPort(config.getAgentPort())
            .vaultPort(config.getVaultPort())
            .maxAgentConnections(1)
            .build()) {

            blockchain.start();

            // add the customer metadata to the blockchain
            ArtifactTypeMetadataAccess metadataAccess = new ArtifactTypeMetadataAccessBlockchain(blockchain);

            // Start the sentinels after the metadata is setup
            thread.start();

            metadataAccess.store(CustomerMetadata.create(), bobKeys).get(5, TimeUnit.SECONDS);

            // add a CUSTOMER_CREATED_TRANSACTION that will trigger the sentinel
            UUID customerId = randomUUID();
            submitTransaction(BlockchainUtils.transactionCertificateBuilder()
                    .artifactId(customerId)
                    .artifactType(CUSTOMER_ARTIFACT_TYPE_ID)
                    .transactionId(randomUUID())
                    .transactionType(CustomerMetadata.CUSTOMER_CREATED_TRANSACTION.getId())
                    .withFields()
                    .addString(FIRST_NAME.getId(), "Bob")
                    .addString(LAST_NAME.getId(), "Jones")
                    .sign(bobKeys.getEntityId(), bobKeys.getSigningKeyPair().getPrivateKey()),
                blockchain);

            // Give the sentinel enough time to observe the transaction
            Thread.sleep(2000);

            // the last transaction for this customer should be a CUSTOMER_WELCOMED_TRANSACTION that
            // was added by the sentinel
            TransactionReader reader = blockchain.findLastTransactionIdForArtifactById(customerId)
                .flatMap(blockchain::findTransactionById)
                .orElseThrow(() -> new RuntimeException("Could not find last transaction for " + customerId));
            UUID lastTransactionType = reader.getFirst(Field.TRANSACTION_TYPE).asUUID();

            System.out.printf("The last transaction type: %s%n",
                metadataAccess
                    .findByArtifactTypeId(CUSTOMER_ARTIFACT_TYPE_ID)
                    .flatMap(md -> md.findTransactionTypeById(lastTransactionType))
                    .map(TransactionType::getName).orElse(null));
        } finally {
            thread.interrupt();
        }
    }

    /**
     * Simulate a separate process by running SentinelContainer in a thread. Normally the SentinelContainer would be run on a server.
     */
    private static Thread sentinelContainerThread(ExamplesConfig config) throws IOException {
        ExamplesConfig.getInstance().getEntityKeysConfigFile("example", true);
        Path configPath = Files.createTempFile("sentinel",".properties");
        // Write a configuration file for the SentinelContainer
        Files.write(configPath, ("agent.host=" + config.getHost() + "\n" +
            "agent.port=" + config.getAgentPort() + "\n" +
            "sentinel.example=example.sentinels.CustomerCreatedSentinel\n" +
            "sentinel.example.keys=" + ExamplesConfig.getInstance().getEntityKeys() + "/example.keyconfig\n").getBytes());
        Path connectionKeys = ExamplesConfig.getInstance().getServiceEntityKeysConfigFile();
        return new Thread(() -> {
            try {
                SentinelContainer.main(new String[]{ configPath.toString(), connectionKeys.toString() });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void submitTransaction(Certificate transaction, BlockchainOperations blockchain) throws Exception {
        TransactionStatus transactionStatus = blockchain.submit(transaction).get(2, TimeUnit.SECONDS);
        if (transactionStatus != TransactionStatus.SUCCEEDED) {
            throw new Exception("Transaction did not succeed");
        }
    }
}
