package example.sentinels;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.crypt.SigningPrivateKey;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.sentinel.ConfigDef;
import com.velopayments.blockchain.sdk.sentinel.Sentinel;
import com.velopayments.blockchain.sdk.sentinel.criteria.Criteria;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static example.sentinels.CustomerMetadata.CUSTOMER_CREATED_TRANSACTION;
import static java.util.UUID.randomUUID;

public class CustomerCreatedSentinel implements Sentinel {

    private BlockchainOperations blockchain;
    private EntityKeys keys;

    public CustomerCreatedSentinel() {
    }

    @Override
    public ConfigDef config() {
        return ConfigDef.builder()
            .key("keys", ConfigDef.Type.STRING)
            .build();
    }

    @Override
    public Criteria start(Map<String, Object> settings, BlockchainOperations blockchain) {
        this.blockchain = blockchain;
        this.keys = EntityTool.fromJson(Path.of((String) settings.get("keys")));
        return Criteria.builder()
            .latestBlockId(blockchain.getLatestBlockId())
            .transactionType(CUSTOMER_CREATED_TRANSACTION.getId())
            .build();
    }

    @Override
    public synchronized Optional<Criteria> notify(UUID latestBlockId, Criteria criteria) {
        blockchain.findAllBlocksAfter(criteria.getLatestBlockId())
            .flatMap(blockReader -> blockReader.getTransactions().stream())
            .filter(transactionReader -> CUSTOMER_CREATED_TRANSACTION.getId().equals(transactionReader.getTransactionType()))
            .forEach(this::processTransactions);

        return criteria.withBlockId(latestBlockId);
    }

    /**
     * Sends a welcome email to the customer and records a CUSTOMER_WELCOMED_TRANSACTION
     *
     * @param reader the transaction that triggered the sentinel
     */
    private void processTransactions(TransactionReader reader) {
        UUID transactionId = reader.getFirst(Field.CERTIFICATE_ID).asUUID();
        UUID customerId = reader.getFirst(Field.ARTIFACT_ID).asUUID();
        String firstname = reader.getFirst(CustomerMetadata.FIRST_NAME.getId()).asString();
        String lastname = reader.getFirst(CustomerMetadata.LAST_NAME.getId()).asString();

        System.out.printf("sending welcome email to customer %s %s%n", firstname, lastname);

        submitTransaction(BlockchainUtils.transactionCertificateBuilder()
                .artifactId(customerId)
                .artifactType(CustomerMetadata.CUSTOMER_ARTIFACT_TYPE_ID)
                .transactionId(randomUUID())
                .transactionType(CustomerMetadata.CUSTOMER_WELCOME_TRANSACTION.getId())
                .previousTransactionId(transactionId)
                .withFields()
                .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey()),
            blockchain);
    }

    private static void submitTransaction(Certificate transaction, BlockchainOperations blockchain) {
        TransactionStatus transactionStatus;
        try {
            transactionStatus = blockchain.submit(transaction).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (transactionStatus != TransactionStatus.SUCCEEDED) {
            throw new RuntimeException("Transaction did not succeed");
        }
    }
}
