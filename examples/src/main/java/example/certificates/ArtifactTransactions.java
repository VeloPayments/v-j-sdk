package example.certificates;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.RemoteBlockchain;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataAccessBlockchain;
import example.ExamplesConfig;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static example.certificates.PersonMetadata.*;
import static java.util.UUID.randomUUID;

/**
 * Demonstration of the use of certificates to define artifact transactions.
 * <ul>
 * <li> defining artifact metadata</li>
 * <li> create an artifact representing a person with attributes firstname, lastname</li>
 * <li> update the artifact by changing the firstname</li>
 * <li> update the artifact by adding a date of birth</li>
 * <li> retrieve all the transactions for our artifact</li>
 * </ul>
 */
public class ArtifactTransactions {

    public static void main(String[] args) throws Exception {
        ExamplesConfig config = ExamplesConfig.getInstance();

        EntityKeys connectionKeys = EntityTool.fromJson(config.getServiceEntityKeysConfigFile());
        RemoteBlockchain blockchain = RemoteBlockchain.builder()
            .entityKeys(connectionKeys)                       // (1)
            .agentHost(config.getHost())                      // (2)
            .agentPort(config.getAgentPort())                 // (3)
            .vaultPort(config.getVaultPort())
            .maxAgentConnections(1)
            .build();

        try (blockchain) {
            blockchain.start();
            // Stores person metadata on the blockchain, allowing it be viewed in Velochain Explorer
            var bobKeys = EntityTool.fromJson(ExamplesConfig.getInstance().getEntityKeysConfigFile("bob", true));
            new ArtifactTypeMetadataAccessBlockchain(blockchain).store(PersonMetadata.create(), bobKeys);

            // example of simple transactions against a blockchain artifact
            doTransactions(blockchain, bobKeys);
        }
    }

    static void doTransactions(BlockchainOperations blockchain, EntityKeys keys) throws Exception {
        // define an artifact with a generated UUID
        final UUID bobArtifactId = randomUUID();     // (1)

        // submit a transaction to represent the artifact creation
        UUID createTransactionId = randomUUID();
        submitTransaction(
            BlockchainUtils.transactionCertificateBuilder()
                .artifactId(bobArtifactId)
                .artifactType(ARTIFACT_TYPE_ID)
                .transactionId(createTransactionId)
                .transactionType(PERSON_CREATED.getId())
                .withFields()
                .addString(FIRST_NAME_FIELD.getId(),"Bob")
                .addString(LAST_NAME_FIELD.getId(),"Sanders")
                .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey()),
            blockchain);

        // submit a second transaction for Bob to update his firstname to Robert
        // Note the "previousTransactionId" is set to the initial transaction to link them.

        UUID updateFirstNameTransactionId = randomUUID();
        submitTransaction(
            BlockchainUtils.transactionCertificateBuilder()
                .artifactId(bobArtifactId)
                .artifactType(ARTIFACT_TYPE_ID)
                .transactionId(updateFirstNameTransactionId)
                .previousTransactionId(createTransactionId)
                .transactionType(PERSON_UPDATED.getId())
                .withFields()
                .addString(FIRST_NAME_FIELD.getId(),"Robert")
                .addString(LAST_NAME_FIELD.getId(),"Sanders")
                .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey()),
            blockchain);

        // submit a third transaction to add Bob's birthdate.
        // Note the "previousTransactionId" is set to the transaction updating his first name.

        UUID updateDobTransactionId = randomUUID();
        Date bobsBirthday = Date.from(LocalDate.of(1975, 3, 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant());

        submitTransaction(
            BlockchainUtils.transactionCertificateBuilder()
                .artifactId(bobArtifactId)
                .artifactType(ARTIFACT_TYPE_ID)
                .transactionId(updateDobTransactionId)
                .previousTransactionId(updateFirstNameTransactionId)
                .transactionType(PERSON_UPDATED.getId())
                .withFields()
                .addString(FIRST_NAME_FIELD.getId(),"Robert")
                .addString(LAST_NAME_FIELD.getId(),"Sanders")
                .addDate(DOB_FIELD.getId(), bobsBirthday)
                .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey()),
            blockchain);

        // FIXME: no sleeps
        Thread.sleep(5 * 1000);

        // read data from the blockchain
        // print most recent transaction for "Bob"
        UUID lastBobTransactionId = blockchain.findLastTransactionIdForArtifactById(bobArtifactId).orElseThrow();
        blockchain.findTransactionById(lastBobTransactionId).ifPresent(reader -> {
            System.out.printf("=== Person artifact %s ==%n", bobArtifactId);
            System.out.printf("Firstname: %s, Lastname: %s, DOB: %s%n%n",
                reader.getFirst(FIRST_NAME_FIELD.getId()).asString(),
                reader.getFirst(LAST_NAME_FIELD.getId()).asString(),
                reader.getFields().contains(DOB_FIELD.getId()) ? reader.getFirst(DOB_FIELD.getId()).asDate().toInstant() : "");
        });

        // retrieve and print all the transactions for "Bob"
        System.out.printf("=== Transaction history for %s ==%n", bobArtifactId);
        findAllTransactionsForArtifact(bobArtifactId, blockchain).forEach(reader ->
            System.out.printf("Transaction %s -> Firstname: %s, Lastname: %s, DOB: %s%n",
                reader.getFirst(Field.CERTIFICATE_ID).asUUID(),
                reader.getFirst(FIRST_NAME_FIELD.getId()).asString(),
                reader.getFirst(LAST_NAME_FIELD.getId()).asString(),
                reader.getFields().contains(DOB_FIELD.getId()) ? reader.getFirst(DOB_FIELD.getId()).asDate().toInstant() : ""));
    }

    private static void submitTransaction(Certificate transaction, BlockchainOperations blockchain) throws Exception {
        // the submit method is a non-blocking, and immediately returns its results as a future TransactionStatus
        CompletableFuture<TransactionStatus> result = blockchain.submit(transaction);

        // allow the future to complete
        TransactionStatus transactionStatus = result.get(5, TimeUnit.SECONDS);
        if (transactionStatus != TransactionStatus.SUCCEEDED) {
            throw new Exception("Transaction did not succeed");
        }
    }

    /**
     * Find each transaction submitted against the artifact, returned a list of CertificateReader objects for each.
     * @param artifactId the id of the artifact to find transactions from
     * @param blockchain  the {@code BlockchainOperations } to search
     * @return list of certificate readers found
     */
    private static List<TransactionReader> findAllTransactionsForArtifact(UUID artifactId, BlockchainOperations blockchain) {
        List<TransactionReader> readers = new ArrayList<>();
        Optional<TransactionReader> transaction = blockchain.findFirstTransactionIdForArtifactById(artifactId)
            .flatMap(blockchain::findTransactionById);
        while (transaction.isPresent()) {
            TransactionReader reader = transaction.get();
            readers.add(reader);
            transaction = blockchain.findNextTransactionIdForTransactionById(reader.getTransactionId())
                .flatMap(blockchain::findTransactionById);
        }
        return readers;
    }
}
