package example.certificates;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.RemoteBlockchain;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder;
import com.velopayments.blockchain.sdk.metadata.FieldMetadata;
import com.velopayments.blockchain.sdk.metadata.FieldType;
import com.velopayments.blockchain.sdk.metadata.MetadataHelper;
import com.velopayments.blockchain.sdk.metadata.SearchOptions;
import com.velopayments.blockchain.sdk.metadata.TransactionType;
import example.ExamplesConfig;
import org.awaitility.Duration;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.extractMetadata;
import static example.certificates.SimpleCertificateRunnable.SimpleArtifactMetadata.BAR_FIELD;
import static example.certificates.SimpleCertificateRunnable.SimpleArtifactMetadata.DISPLAY_NAME;
import static example.certificates.SimpleCertificateRunnable.SimpleArtifactMetadata.FOO_FIELD;
import static example.certificates.SimpleCertificateRunnable.SimpleArtifactMetadata.SIMPLE_ARTIFACT_TYPE_ID;
import static example.certificates.SimpleCertificateRunnable.SimpleArtifactMetadata.SIMPLE_TRANSACTION_TYPE;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;

public class SimpleCertificateRunnable implements Runnable {
    private final int repeat;

    public SimpleCertificateRunnable(int repeat) {
        this.repeat = repeat;
    }

    @Override
    public void run() {
        ExamplesConfig config = ExamplesConfig.getInstance();
        EntityKeys connectionKeys = EntityTool.fromJson(config.getServiceEntityKeysConfigFile());

        RemoteBlockchain blockchain = RemoteBlockchain.builder()
            .entityKeys(connectionKeys)                   // (1)
            .agentHost(config.getHost())                  // (2)
            .agentPort(config.getAgentPort())             // (3)
            .vaultPort(config.getVaultPort())
            .maxAgentConnections(1)
            .build();

        final EntityKeys bobKeys = EntityTool.fromJson(config.getEntityKeysConfigFile("bob", true));
        try (blockchain) {
            blockchain.start();
            MetadataHelper.initMetadata(blockchain, bobKeys, SimpleCertificateRunnable.SimpleArtifactMetadata.create()
                .withSearchOptions(SearchOptions.FullTextNonEncryptedFieldValues));

            try {
                for (int i = 0; i < repeat; i++) {
                    submitTransaction(blockchain, bobKeys);
                    System.out.println();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create and submit a simple unsigned certificate describing a blockchain transaction.
     *
     * Notes for code below:
     * <pre>
     * (1) - Create a builder for a transaction certificate
     * (2) - Defines the id for the transaction
     * (3) - Identifies the type of transaction
     * (4) - Identifies an artifact which the transaction applies to (random for a new artifact)
     * (5) - Identifies the type of artifact
     * (6) - withFields() returns a CertificateBuilder to define any custom fields for the transaction
     * (7) - Fields can have multiple values
     * (8) - Builds an immutable transaction Certificate
     *
     * (9) - Submit is a non-blocking, and immediately returns its results as a future TransactionStatus
     * (10) - Allow the future to complete (the duration for this is effected by the configured LocalBlockchain commit
     *       frequency)
     * </pre>
     * @param blockchain the blockchain to submit to
     */
    private static void submitTransaction(BlockchainOperations blockchain, EntityKeys keys) throws Exception {

        // create a certificate
        UUID transactionId = randomUUID();
        UUID artifactId = randomUUID();
        Certificate transaction = BlockchainUtils.transactionCertificateBuilder()  // (1)
            .transactionId(transactionId)                       // (2)
            .transactionType(SIMPLE_TRANSACTION_TYPE.getId())   // (3)
            .artifactId(artifactId)                             // (4)
            .artifactType(SIMPLE_ARTIFACT_TYPE_ID)              // (5)
            .withFields()                                       // (6)
            .addString(FOO_FIELD.getId(), "A")           // (7)
            .addString(FOO_FIELD.getId(), "B")
            .addString(DISPLAY_NAME.getId(), "simple certificate")
            .addLong(BAR_FIELD.getId(), 100000L)
            .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey());  // (8)

        UUID firstId = blockchain.getLatestBlockId();
        System.out.println("Initial Block UUID: " + firstId);

        // store the transaction
        CompletableFuture<TransactionStatus> result = blockchain.submit(transaction);   // (9)
        TransactionStatus transactionStatus = result.get(2, TimeUnit.SECONDS);  // (10)
        if (transactionStatus != TransactionStatus.SUCCEEDED) {
            throw new RuntimeException("Transaction failed. status: " + transactionStatus);
        }

        // retrieve the certificate from back from the blockchain
        await("find_transaction")
            .pollDelay(Duration.ONE_SECOND)
            .pollInterval(Duration.ONE_SECOND)
            .atMost(Duration.ONE_MINUTE).until(() ->
            blockchain.findTransactionById(transactionId).isPresent());

        System.out.println("Found transaction :" + transactionId);
        // parse read the certificate
        TransactionReader reader = blockchain.findTransactionById(transactionId).orElseThrow();
        System.out.printf("field %s has %d values%n", FOO_FIELD, reader.count(FOO_FIELD.getId()));
        System.out.printf("field %s: %s%n", FOO_FIELD, reader.get(FOO_FIELD.getId(), 0).asString());
        System.out.printf("field %s: %s%n", FOO_FIELD, reader.get(FOO_FIELD.getId(), 1).asString());
        System.out.printf("field %s: %d%n", BAR_FIELD, reader.getFirst(BAR_FIELD.getId()).asLong());
        System.out.printf("field %s: %s%n", DISPLAY_NAME, reader.getFirst(DISPLAY_NAME.getId()).asString());
    }

    public static class SimpleArtifactMetadata {
        public static final UUID SIMPLE_ARTIFACT_TYPE_ID = UUID.fromString("898bcde7-bed4-4841-9365-c16b5ded7818");

        public static final TransactionType SIMPLE_TRANSACTION_TYPE = new TransactionType(UUID.fromString("b531476e-3307-4e0d-8647-8e5551f22e2e"), "SIMPLE_TRANSACTION_TYPE");

        public static final FieldMetadata FOO_FIELD = new FieldMetadata(700, "FOO_FIELD", FieldType.String, false, false, 230, true);
        public static final FieldMetadata BAR_FIELD = new FieldMetadata(701, "BAR_FIELD", FieldType.Long, false, false, 240, true);
        public static final FieldMetadata DISPLAY_NAME = new FieldMetadata(0x0507, "DISPLAY_NAME", FieldType.String, false, false, 1000, true);

        public static ArtifactTypeMetadataBuilder create() {
            return extractMetadata(SIMPLE_ARTIFACT_TYPE_ID, "SIMPLE_ARTIFACT", SimpleCertificateRunnable.SimpleArtifactMetadata.class);
        }
    }
}
