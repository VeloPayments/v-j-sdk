package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.CertificateBuilder;
import com.velopayments.blockchain.cert.CertificateParser;
import com.velopayments.blockchain.cert.CertificateReader;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.guard.PreSubmitGuard;
import com.velopayments.blockchain.sdk.vault.ExternalReference;
import com.velopayments.blockchain.sdk.vault.MutableFields;
import com.velopayments.blockchain.sdk.vault.VaultUtils;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Duration;
import org.awaitility.core.ThrowingRunnable;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockserver.mockserver.MockServer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.velopayments.blockchain.cert.CertificateType.ROOT_BLOCK;
import static com.velopayments.blockchain.sdk.BlockchainUtils.INITIAL_TRANSACTION_UUID;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

@Slf4j
public class RemoteBlockchainTest {

    private static UUID DUMMY_TRANSACTION_TYPE = randomUUID();
    private static UUID DUMMY_ARTIFACT_TYPE = randomUUID();

    @ClassRule
    public static GenericContainer<?> agentdContainer = TestUtils.agentdContainer();

    @ClassRule
    public static GenericContainer<MockServerContainer> vaultContainer = TestUtils.vaultContainer();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static RemoteBlockchain blockchain;

    @BeforeClass
    public static void setUp() throws Exception {
        blockchain = TestUtils.createTestBlockchain(agentdContainer, vaultContainer);
        blockchain.start();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        blockchain.close();
    }

    private EntityKeys entityKeys = EntityTool.generate("TEST");

    @Test
    public void previousBlockIdEndsWithRootBlock() throws Exception {
        var futureResult = blockchain.submit(dummyTransaction(randomUUID()));
        assertThat(futureResult.get(8, TimeUnit.SECONDS))
            .isEqualTo(TransactionStatus.SUCCEEDED);

        UUID latest = blockchain.getLatestBlockId();
        int i = 0; // prevent infinite loop
        while (!latest.equals(ROOT_BLOCK) && i < 100) {
            latest = blockchain.findPrevBlockId(latest).get();
            ++i;
        }

        assertThat(latest).isEqualTo(ROOT_BLOCK);
    }

    @Test
    public void submitAndFindTransaction() throws Exception {
        var transactionId = randomUUID();
        var transaction = dummyTransaction(transactionId);
        var futureResult = blockchain.submit(transaction);

        assertThat(futureResult).isNotNull();
        assertThat(futureResult.get(8, TimeUnit.SECONDS))
            .isEqualTo(TransactionStatus.SUCCEEDED);

        awaitCanonized(() -> assertThat(blockchain.findTransactionById(transactionId)).hasValueSatisfying(txn -> {
                assertThat(txn).isNotNull();
                assertThat(txn.getTransactionId()).isEqualTo(transactionId);
        }));
    }

    @Ignore // FIXME: parallel blockain
    @Test
    public void submitTransactionsParallel() throws Exception {
        int count = 20;
        List<TransactionStatus> results = Stream.generate(UUID::randomUUID)
            .limit(count)
            .parallel()
            .map(this::dummyTransaction)
            .map(cert -> blockchain.submit(cert).exceptionally(ex -> {
                log.error("Submit failed", ex);
                return TransactionStatus.CANCELED;
            }))
            .map(f -> {
                try {
                    return f.get(2, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                    log.error("Future failed to complete", ex);
                    return TransactionStatus.CANCELED;
                }
            })
            .collect(toList());

        assertThat(results)
            .hasSize(count)
            .containsOnly(TransactionStatus.SUCCEEDED);
    }

    @Test
    public void submitTransactionGuarded() throws Exception {
        var exceptionMsg = "These are not the transactions we're looking for: ";
        PreSubmitGuard guard = (txn, blockchain) -> {
            UUID txnType = txn.getTransactionType();
            if (txnType.equals(DUMMY_TRANSACTION_TYPE)) {
                // yep, I like it
            } else {
                throw new RuntimeException(exceptionMsg + txnType);
            }
        };
        blockchain.register(guard);

        var transactionId = randomUUID();
        var transaction = dummyTransaction(transactionId);
        var transactionResult = blockchain.submit(transaction);

        // expect dummy transaction success
        assertThat(transactionResult).isNotNull();
        assertThat(transactionResult.get(8, TimeUnit.SECONDS))
            .isEqualTo(TransactionStatus.SUCCEEDED);

        awaitCanonized(() -> {
            // dummy transaction is canonized
            assertThat(blockchain.findTransactionById(transactionId)).hasValueSatisfying(txn -> {
                assertThat(txn).isNotNull();
                assertThat(txn.getTransactionId()).isEqualTo(transactionId);
            });
        });

        var invalidTransactionId = randomUUID();
        var invalidTransactionType = randomUUID();
        var invalidTransaction = transactionOfType(invalidTransactionId, invalidTransactionType);
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(exceptionMsg + invalidTransactionType);
        blockchain.submit(invalidTransaction);
    }

    @Test @Ignore // FIXME: vault
    public void submitTransactionWithMutableFields() throws Exception {
        final int fieldAId = 700;
        final int fieldBId = 701;
        final int fieldCId = 703;

        // create an initial transaction so that the artifact exists
        UUID artifactId = randomUUID();
        UUID firstTransactionId = randomUUID();
        Certificate firstTransaction = BlockchainUtils.transactionCertificateBuilder()
            .artifactType(DUMMY_TRANSACTION_TYPE)
            .artifactId(artifactId)
            .transactionType(DUMMY_TRANSACTION_TYPE)
            .transactionId(firstTransactionId)
            .previousTransactionId(INITIAL_TRANSACTION_UUID)
            .withFields()
            .sign(entityKeys.getEntityId(), entityKeys.getSigningKeyPair().getPrivateKey());
        TransactionStatus transactionStatus = blockchain.submit(firstTransaction).get(3, TimeUnit.SECONDS);
        assertThat(transactionStatus).isEqualTo(TransactionStatus.SUCCEEDED);

        awaitCanonized(() -> assertThat(blockchain.findTransactionById(firstTransactionId)).isNotEmpty());

        // create external-references to a mutable fields certificate
        long now = System.currentTimeMillis();
        Certificate mutableFieldsCert = CertificateBuilder.createCertificateFragmentBuilder()
            .addString(fieldAId, "FOO")
            .addLong(fieldBId, now)
            .sign(entityKeys.getEntityId(), entityKeys.getSigningKeyPair().getPrivateKey());
        byte[] mutableFields = mutableFieldsCert.toByteArray();
        byte[] mutableFieldsSignature = ExternalReference.createMessageDigest().digest(mutableFields); //TODO: change to using signature
        Certificate externalReference1 = MutableFields.externalReferenceBuilder()
            .artifactId(artifactId)
            .referenceId(randomUUID())
            .schemaFrom(mutableFieldsCert)
            .contentLength((long) mutableFields.length)
            .signature(mutableFieldsSignature)
            .withFields()
            .sign(entityKeys.getEntityId(), entityKeys.getSigningKeyPair().getPrivateKey());
        Certificate signedVaultRef1 = blockchain.addExternalReference(externalReference1, new ByteArrayInputStream(mutableFields)).get(3, TimeUnit.SECONDS);
        assertThat(signedVaultRef1).isNotNull();

        // create external-references to a document
        byte[] document = "{\"value\": \"BAR\"}".getBytes();
        Certificate externalReference2 = VaultUtils.externalReferenceBuilder()
            .artifactId(artifactId)
            .referenceId(randomUUID())
            .anchorField(fieldCId)
            .contentType("application/json")
            .contentLength((long) document.length)
            .signature(ExternalReference.createMessageDigest().digest(document))
            .withFields()
            .sign(entityKeys.getEntityId(), entityKeys.getSigningKeyPair().getPrivateKey());
        Certificate signedVaultRef2 = blockchain.addExternalReference(externalReference2, new ByteArrayInputStream(document)).get(3, TimeUnit.SECONDS);
        assertThat(signedVaultRef2).isNotNull();

        // include the external-references in a second artifact transaction
        UUID transactionId = randomUUID();
        Certificate transaction = BlockchainUtils.transactionCertificateBuilder()
            .artifactType(DUMMY_TRANSACTION_TYPE)
            .artifactId(artifactId)
            .transactionType(DUMMY_TRANSACTION_TYPE)
            .transactionId(transactionId)
            .previousTransactionId(firstTransactionId)
            .withFields()
            .addByteArray(VaultUtils.EXTERNAL_REF_SIGNED, signedVaultRef1.toByteArray())
            .addByteArray(VaultUtils.EXTERNAL_REF_SIGNED, signedVaultRef2.toByteArray())
            .sign(entityKeys.getEntityId(), entityKeys.getSigningKeyPair().getPrivateKey());
        transactionStatus = blockchain.submit(transaction).get(3, TimeUnit.SECONDS);
        assertThat(transactionStatus).isEqualTo(TransactionStatus.SUCCEEDED);

        // load the data from the mutable fields
        assertThat(blockchain.findTransactionById(transactionId)).hasValueSatisfying(txnReader -> {
            List<Certificate> externalReferences = txnReader.getExternalReferences();
            assertThat(externalReferences).hasSize(2);

            ExternalReference externalReference = blockchain.resolveExternalReference(externalReferences.get(0));
            assertThat(externalReference).isNotNull();
            assertThat(externalReference.getExternalReferenceTypeId()).isEqualTo(MutableFields.MUTABLE_FIELDS_CERT_TYPE_ID);
            assertThat(externalReference.isPresent()).isTrue();
            assertThat(externalReference.getContentLength()).isEqualTo(mutableFields.length);
            assertThat(externalReference.getSignatureEncoded())
                .isEqualTo(Base64.getEncoder().encodeToString(mutableFieldsSignature));
            assertThat(externalReference.getOriginalFileName()).isNotPresent();
            assertThat(externalReference.hasExternalField(fieldAId)).isTrue();
            assertThat(externalReference.hasExternalField(fieldBId)).isTrue();
            assertThat(externalReference.hasExternalField(999)).isFalse();

            CertificateReader extRefCertReader = new CertificateReader(new CertificateParser(externalReference.getCertificate()));
            assertThat(extRefCertReader.count(VaultUtils.EXTERNAL_REF_ANCHOR_FIELD_ID))
                .as("There should be anchors for fieldAId abd fieldBId")
                .isEqualTo(2);
            assertThat(extRefCertReader.get(VaultUtils.EXTERNAL_REF_ANCHOR_FIELD_ID, 0).asInt())
                .isEqualTo(fieldAId);
            assertThat(extRefCertReader.get(VaultUtils.EXTERNAL_REF_ANCHOR_FIELD_ID, 1).asInt())
                .isEqualTo(fieldBId);

            try {
                TransactionReader mutableFieldsReader = new TransactionReader(Certificate.fromByteArray(externalReference.asByteArray()));
                assertThat(mutableFieldsReader).isNotNull();
                assertThat(mutableFieldsReader.getFirst(fieldAId).asString()).isEqualTo("FOO");
                assertThat(mutableFieldsReader.getFirst(fieldBId).asLong()).isEqualTo(now);
            } catch (IOException e) {
                fail("Cannot load mutable fields", e);
            }
        });
    }

    private Certificate dummyTransaction(UUID transactionId) {
        return transactionOfType(transactionId, DUMMY_TRANSACTION_TYPE);
    }

    private Certificate transactionOfType(UUID transactionId, UUID transactionType) {
        return transactionOfType(transactionId, transactionType, randomUUID(), INITIAL_TRANSACTION_UUID);
    }

    private Certificate transactionOfType(UUID transactionId, UUID transactionType, UUID artifactId, UUID previousTransactionId) {
        return BlockchainUtils.transactionCertificateBuilder()
            .transactionId(transactionId)
            .previousTransactionId(previousTransactionId)
            .transactionType(transactionType)
            .artifactId(artifactId)
            .artifactType(DUMMY_ARTIFACT_TYPE)
            .timestamp(System.currentTimeMillis())
            .previousState(0)
            .newState(1)
            .withFields()
            .sign(entityKeys.getEntityId(), entityKeys.getSigningKeyPair().getPrivateKey());
    }

    @Test
    public void testFindAllBlocksAfter() throws Exception {
        var startBlock = blockchain.getLatestBlockId();
        IntStream.range(1,10).forEach(i -> blockchain.submit(dummyTransaction(randomUUID())));

        awaitCanonized(() -> {
            assertThat(blockchain.getLatestBlockId())
                .isNotEqualTo(startBlock).as("Latest block should advance");

            //find all blocks
            List<BlockReader> allBlocks = blockchain.findAllBlocksAfter(startBlock).collect(toList());
            assertThat(allBlocks.size()).isGreaterThan(0);
        });
    }

    @Test
    public void testVariousFindMethods() throws Exception {
        var artifactId = randomUUID();

        var transactionId1 = randomUUID();
        var startBlock = blockchain.getLatestBlockId();
        blockchain.submit(transactionOfType(transactionId1, DUMMY_TRANSACTION_TYPE, artifactId, INITIAL_TRANSACTION_UUID)).get();

        awaitCanonized(() -> assertThat(blockchain.getLatestBlockId()).isNotEqualTo(startBlock));
        var blockId1 = blockchain.getLatestBlockId();

        var transactionId2= randomUUID();
        blockchain.submit(transactionOfType(transactionId2, DUMMY_TRANSACTION_TYPE, artifactId, transactionId1)).get();

        awaitCanonized(() -> {
            var blockId2 = blockchain.getLatestBlockId();
            assertNotEquals(blockId1, blockId2);

            assertEquals(transactionId2, blockchain.findLastTransactionIdForArtifactById(artifactId).get());
            assertEquals(Optional.empty(), blockchain.findLastTransactionIdForArtifactById(randomUUID()));

            //assertEquals(blockId2, template.findLastBlockIdForArtifactById(artifactId).get());    waiting on BLOC-179
            //assertEquals(Optional.empty(), template.findLastBlockIdForArtifactById(randomUUID()));

            assertEquals(blockId1, blockchain.findTransactionBlockId(transactionId1).get());
            assertEquals(blockId2, blockchain.findTransactionBlockId(transactionId2).get());
            assertEquals(Optional.empty(), blockchain.findTransactionBlockId(randomUUID()));

            assertEquals(blockId2, blockchain.findNextBlockId(blockId1).get());
            assertEquals(blockId1, blockchain.findPrevBlockId(blockId2).get());
            assertEquals(Optional.empty(), blockchain.findPrevBlockId(randomUUID()));


            assertEquals(Optional.empty(), blockchain.findBlockById(randomUUID()));
            var blockReader = blockchain.findBlockById(blockId1).get();

            assertTrue(blockReader.getTransactions().stream()
                .map(TransactionReader::getTransactionId)
                .collect(toList())
                .contains(transactionId1));

            long block1Height = blockReader.getBlockHeight();
            assertEquals(blockId1, blockchain.findBlockIdByBlockHeight(block1Height).get());
            assertEquals(blockId2, blockchain.findBlockIdByBlockHeight(block1Height + 1).get());
            assertEquals(Optional.empty(), blockchain.findBlockIdByBlockHeight(block1Height + 2));

            assertEquals(transactionId2, blockchain.findNextTransactionIdForTransactionById(transactionId1).get());
            assertEquals(Optional.empty(), blockchain.findNextTransactionIdForTransactionById(transactionId2));

            assertEquals(transactionId1, blockchain.findPreviousTransactionIdForTransactionById(transactionId2).get());
            assertEquals(Optional.empty(), blockchain.findPreviousTransactionIdForTransactionById(transactionId1));

            assertEquals(transactionId1, blockchain.findFirstTransactionIdForArtifactById(artifactId).get());
            assertEquals(Optional.empty(), blockchain.findFirstTransactionIdForArtifactById(randomUUID()));

            assertEquals(transactionId2, blockchain.findLastTransactionIdForArtifactById(artifactId).get());
            assertEquals(Optional.empty(), blockchain.findLastTransactionIdForArtifactById(randomUUID()));
        });
    }

    private void awaitCanonized(ThrowingRunnable function) {
        await("transaction")
            .pollDelay(Duration.ONE_SECOND)
            .pollInterval(Duration.TWO_HUNDRED_MILLISECONDS)
            .atMost(Duration.TEN_SECONDS).untilAsserted(function);
    }
}
