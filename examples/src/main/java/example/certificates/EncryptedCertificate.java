package example.certificates;

import com.velopayments.blockchain.cert.*;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.*;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityMetadata;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.metadata.MetadataHelper;
import example.ExamplesConfig;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static example.certificates.PersonMetadata.*;
import static java.util.UUID.randomUUID;

/**
 * A demonstration of the use of encrypted fields and signed certificates.
 * <ul>
 * <li> defining entities with encryption and signing key pairs</li>
 * <li> creating a signed certificate that contains both plain and encrypted fields</li>
 * <li> defining an audience of entities which can decrypt values - a shared secret</li>
 * <li> retrieving the certificate from the blockchain and reading the fields</li>
 * </ul>
 */
public class EncryptedCertificate {

    /**
     * Notes for code below:
     * <pre>
     * (1) - Provides a unique identifier of the entity creating transactions along with its encryption and signing keys
     * (2) - Host name for agentd service
     * (3) - Port for agentd service
     * (4) - Stores person metadata on the blockchain, allowing it be viewed in Velochain Explorer
     * </pre>
     * @param args the command line parameters
     * @throws Exception on unhandled errors
     */
    public static void main(String[] args) throws Exception {
        Integer repeat = Stream.of(args).filter(s -> s.length() > 7 && s.startsWith("repeat="))
            .map(s -> Integer.parseInt(s.substring(7).trim()))
            .findFirst().orElse(1);
        ExamplesConfig config = ExamplesConfig.getInstance();

        EntityKeys connectionKeys = EntityTool.fromJson(config.getServiceEntityKeysConfigFile());
        RemoteBlockchain blockchain = RemoteBlockchain.builder()
            .entityKeys(connectionKeys)                        // (1)
            .agentHost(config.getHost())                       // (2)
            .agentPort(config.getAgentPort())                  // (3)
            .vaultPort(config.getVaultPort())
            .maxAgentConnections(1)
            .build();

        try (blockchain) {
            blockchain.start();
            MetadataHelper.initMetadata(blockchain, connectionKeys, PersonMetadata.create());    // (4)

            for (int i = 0; i < repeat; i++) {
                submitTransaction(blockchain);
                System.out.println();
            }
        }
    }

    /**
     * Create and submit a simple unsigned certificate describing a blockchain transaction.
     *
     * Notes for code below:
     * <pre>
     * (1) - Creates an encrypted certificate builder for a transaction
     * (2) - Provides the key pair of the entity encrypting the transaction
     * (3) - withFields() returns an EncryptedCertificateBuilder to define any custom fields for the transaction
     * (4) - Defines Alice as a subscriber of a shared secret by giving her entity id and encryption public key
     * (5) - Defines Sam as a second subscriber of the shared secret with his entity id and encryption public key
     * (6) - Provides a ACCOUNT_NUM_FIELD value that will be encrypted on the transaction
     * (7) - Provides a FOR_ALL value that will be an unencrypted field on the transaction
     * (8) - Builds an immutable transaction Certificate, signed by the certificate builder
     *
     * (9) - Throws an exception. Encrypted certs can be read with a CertificateReader, but only unencrypted fields.
     * (10) - An EncryptedCertificateReader will give access to all fields
     * (11) - Provides the entity id of the shared secret subscriber reading the certificate
     * (12) - Provides the private key of the shared secret subscriber reading the certificate
     * (13) - Provides the public key of the entity tha created the certificate
     * (14) - The reader must load the secret key before reading values
     * </pre>
     * @param blockchain the blockchain to submit to
     */
    private static void submitTransaction(BlockchainOperations blockchain) throws Exception {

        ExamplesConfig config = ExamplesConfig.getInstance();
        final EntityKeys bob = EntityTool.fromJson(config.getEntityKeysConfigFile("bob", true));
        final EntityKeys alice = EntityTool.fromJson(config.getEntityKeysConfigFile("alice", true));
        final EntityKeys sam = EntityTool.fromJson(config.getEntityKeysConfigFile("sam", true));
        registerEntityForKeys(blockchain, bob, bob);
        registerEntityForKeys(blockchain, alice, alice);
        registerEntityForKeys(blockchain, sam, sam);

        // create a certificate
        UUID transactionId = randomUUID();

        Certificate transaction = BlockchainUtils.encryptedTransactionCertificateBuilder()              // (1)
            .encryptionKeyPair(bob.getEncryptionKeyPair())                                              // (2)
            .transactionId(transactionId)
            .transactionType(PERSON_CREATED.getId())
            .artifactId(sam.getEntityId())
            .artifactType(ARTIFACT_TYPE_ID)
            .withFields()                                                                               // (3)
            .addEncryptedSharedSecret(alice.getEntityId(), alice.getEncryptionKeyPair().getPublicKey()) // (4)
            .addEncryptedSharedSecret(sam.getEntityId(), sam.getEncryptionKeyPair().getPublicKey())     // (5)
            .addEncryptedString(ACCOUNT_NUM_FIELD.getId(), "4111111111111119")                    // (6)
            .addEncryptedInt(LUCKY_NUMBER_FIELD.getId(), 42)
            .addString(FIRST_NAME_FIELD.getId(), "Sam")                                           // (7)
            .addString(LAST_NAME_FIELD.getId(), "Sandwich")
            .sign(bob.getEntityId(), bob.getSigningKeyPair().getPrivateKey());                          // (8)

        // store the transaction
        CompletableFuture<TransactionStatus> result = blockchain.submit(transaction);
        TransactionStatus transactionStatus = result.get(2, TimeUnit.SECONDS);
        if (transactionStatus != TransactionStatus.SUCCEEDED) {
            throw new RuntimeException("Person created transaction failed.  status: " + transactionStatus);
        }

        System.out.printf("Created transaction %s with encrypted account number field(%s)%n%n", transactionId, ACCOUNT_NUM_FIELD.getId());

        Thread.sleep(2 * 1000);

        // retrieve the certificate
        TransactionReader reader = blockchain.findTransactionById(transactionId)
            .orElseThrow(() -> new RuntimeException("Expected to find transaction " + transactionId));

        // read the certificate without keys
        try {
            // because ACCOUNT_NUM is encrypted, it cannot be read as String
            reader.getFirst(ACCOUNT_NUM_FIELD.getId()).asString();                                      // (9)
            throw new IllegalStateException("Should be unreachable - field is encrypted");
        } catch (FieldConversionException ex) {
            System.out.println(ex.getMessage());
        }

        System.out.println("CertificateReader decrypted fields only");
        System.out.println("=======================================");
        System.out.printf("%s: %s%n", FIRST_NAME_FIELD.getName(), reader.getFirst(FIRST_NAME_FIELD.getId()).asString());
        System.out.printf("%s: %s%n", LAST_NAME_FIELD.getName(), reader.getFirst(LAST_NAME_FIELD.getId()).asString());
        System.out.println();

        // read the certificate Alice's key
        EncryptedCertificateReader aliceReader = new EncryptedCertificateReader(                      // (10)
            alice.getEntityId(),                                                                      // (11)
            alice.getEncryptionKeyPair().getPrivateKey(),                                             // (12)
            bob.getEncryptionKeyPair().getPublicKey(),                                                // (13)
            new CertificateParser(reader.getCertificate()));
        aliceReader.loadSecretKey();                                                                  // (14)
        System.out.println("EncryptedCertificateReader as Alice");
        System.out.println("=======================================");
        System.out.printf("%s: %s%n", FIRST_NAME_FIELD.getName(), aliceReader.getFirst(FIRST_NAME_FIELD.getId()).asString());
        System.out.printf("%s: %s%n", LAST_NAME_FIELD.getName(), aliceReader.getFirst(LAST_NAME_FIELD.getId()).asString());
        System.out.printf("%s: %s%n", ACCOUNT_NUM_FIELD.getName(), aliceReader.getFirstEncrypted(ACCOUNT_NUM_FIELD.getId()).asString());
        System.out.printf("%s: %s%n", LUCKY_NUMBER_FIELD.getName(), aliceReader.getFirstEncrypted(LUCKY_NUMBER_FIELD.getId()).asInt());
    }

    private static void registerEntityForKeys(BlockchainOperations blockchain, EntityKeys entity, Signer signer) {
        // TODO: allow updates
        UUID transactionId = UUID.randomUUID();
        if (blockchain.findTransactionById(entity.getEntityId()).isEmpty()) {
            Certificate register = BlockchainUtils.transactionCertificateBuilder()
                .artifactType(EntityMetadata.ARTIFACT_TYPE_ID)
                .transactionId(transactionId)
                .transactionType(EntityMetadata.ENTITY_CREATED.getId())
                .artifactId(entity.getEntityId())
                .withFields()
                .addString(EntityMetadata.ENTITY_NAME.getId(), entity.getEntityName())
                .addByteArray(Field.PUBLIC_ENCRYPTION_KEY, entity.getEncryptionKeyPair().getPublicKey().getRawBytes())
                .addByteArray(Field.PUBLIC_SIGNING_KEY, entity.getSigningKeyPair().getPublicKey().getRawBytes())
                .sign(signer.getEntityId(), signer.getSigningKeyPair().getPrivateKey());
            CompletableFuture<TransactionStatus> result = blockchain.submit(register);
            try {
                TransactionStatus status = result.get(2, TimeUnit.SECONDS);
                if (status != TransactionStatus.SUCCEEDED) {
                    throw new RuntimeException("Register entity transaction failed for" + entity.getEntityName() + ". status: " + status);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error completing transaction", e);
            }
        }
    }
}
