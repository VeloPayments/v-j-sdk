package example.guards;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.RemoteBlockchain;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder;
import com.velopayments.blockchain.sdk.metadata.FieldMetadata;
import com.velopayments.blockchain.sdk.metadata.FieldType;
import com.velopayments.blockchain.sdk.metadata.TransactionType;
import example.ExamplesConfig;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;
import static com.velopayments.blockchain.sdk.metadata.MetadataHelper.initMetadata;
import static example.guards.GuardExample.GuardExampleMetadata.*;
import static java.util.UUID.randomUUID;

/**
 * Simple demonstration of:
 *
 * (1) creating a blockchain with a local connection
 * (2) add a guard that ensures balances remain non-negative
 * (3) create a transaction that will be rejected by the guard
 * (4) submit the transaction
 *
 */
public class GuardExample {

    public static void main(String[] args) throws Exception {
        ExamplesConfig config = ExamplesConfig.getInstance();

        EntityKeys connectionKeys = EntityTool.fromJson(config.getServiceEntityKeysConfigFile());
        RemoteBlockchain blockchain = RemoteBlockchain.builder()
            .entityKeys(connectionKeys)
            .agentHost(config.getHost())
            .agentPort(config.getAgentPort())
            .vaultPort(config.getVaultPort())
            .maxAgentConnections(1)
            .build();

        EntityKeys aliceKeys = EntityTool.fromJson(config.getEntityKeysConfigFile("alice", true));

        try (blockchain) {
            blockchain.start();
            initMetadata(blockchain, aliceKeys, GuardExampleMetadata.create());

            // create a simple guard and register it
            blockchain.register(new NegativeBalanceGuard()); // note: could also be added via the builder

            // add a transaction that is not allowed by the guard
            UUID transactionId = randomUUID();
            Certificate transaction = BlockchainUtils.transactionCertificateBuilder()
                .artifactId(randomUUID())
                .artifactType(ARTIFACT_TYPE_ID)
                .transactionId(transactionId)
                .transactionType(TRANSACTION_TYPE.getId())
                .withFields()
                .addLong(BALANCE_FIELD.getId(), -10)
                .sign(aliceKeys.getEntityId(), aliceKeys.getSigningKeyPair().getPrivateKey());

            try {
                blockchain.submit(transaction).get(2, TimeUnit.SECONDS);
            } catch (NegativeBalanceException e) {
                System.out.printf("Transaction rejected: %s%n", e.getMessage());
            }
        }
    }

    public static class GuardExampleMetadata {
        public static final UUID ARTIFACT_TYPE_ID = UUID.fromString("73f4bfd1-c170-45d3-acef-d3e746905bf1");

        public static final TransactionType TRANSACTION_TYPE = new TransactionType(UUID.fromString("6adb8602-f891-445e-99a2-96cdb03ad750"), "BANNED_TRANSACTION");

        public static final FieldMetadata BALANCE_FIELD = new FieldMetadata(700, "BALANCE_FIELD", FieldType.Long, false, false, 240);

        public static ArtifactTypeMetadataBuilder create() {
            return extractMetadata(ARTIFACT_TYPE_ID, "GUARD_EXAMPLE", GuardExampleMetadata.class);
        }
    }
}
