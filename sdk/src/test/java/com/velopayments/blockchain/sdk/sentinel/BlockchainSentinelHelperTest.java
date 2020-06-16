package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.BlockReader;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.RemoteBlockchain;
import com.velopayments.blockchain.sdk.TestUtils;
import org.awaitility.Duration;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.velopayments.blockchain.cert.CertificateType.ROOT_BLOCK;
import static com.velopayments.blockchain.sdk.sentinel.PayeeMetadata.PAYEE_CREATED;
import static com.velopayments.blockchain.sdk.sentinel.PayeeMetadata.PAYEE_TYPE_ID;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class BlockchainSentinelHelperTest {

    @ClassRule
    public static GenericContainer<?> agentdContainer = TestUtils.agentdContainer();

    @ClassRule
    public static GenericContainer<MockServerContainer> vaultContainer = TestUtils.vaultContainer();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static RemoteBlockchain blockchain;

    BlockchainSentinelHelper helper;

    @BeforeClass
    public static void setUp() throws Exception {
        blockchain = TestUtils.createTestBlockchain(agentdContainer, vaultContainer);
        blockchain.start();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        blockchain.close();
    }

    @Before
    public void setup() throws Exception {
        helper = new BlockchainSentinelHelper(blockchain);
    }

    @Test
    public void  findTransactionsForArtifact()  throws Exception {
        var startingBlock = blockchain.getLatestBlockId();

        var initalTransaction = createTransaction(randomUUID(), randomUUID(), BlockchainUtils.INITIAL_TRANSACTION_UUID);  //create a random transaction
        awaitCanonization(initalTransaction);

        var blockBeforeStartOfTransactions = blockchain.getLatestBlockId();

        var artifactId = randomUUID();
        var transaction1 = createTransaction(artifactId, randomUUID(), BlockchainUtils.INITIAL_TRANSACTION_UUID);
        awaitCanonization(transaction1);
        var tx1Block = blockchain.getLatestBlockId();

        var transaction2 = createTransaction(artifactId, randomUUID(), transaction1);
        awaitCanonization(transaction2);
        var tx2Block = blockchain.getLatestBlockId();

        var transaction3 = createTransaction(artifactId, randomUUID(), transaction2);
        awaitCanonization(transaction3);
        var tx3Block = blockchain.getLatestBlockId();

        var transaction4 = createTransaction(artifactId, randomUUID(), transaction3);
        awaitCanonization(transaction4);
        var tx4Block = blockchain.getLatestBlockId();

        var allBlockIds = blockchain.findAllBlocksAfter(startingBlock).map(BlockReader::getBlockId).collect(toList());
        assertThat(allBlockIds.size()).isGreaterThanOrEqualTo(5);

        assertThat(helper.findTransactionsForArtifact(randomUUID(), null).getResult()).isEmpty();   //won't find any transactions for this artifact;

        var allTransactions = helper.findTransactionsForArtifact(artifactId, null);
        assertThat(allTransactions.getResult()).hasSize(4);
        assertThat(allTransactions.getLatestBlockId()).isEqualTo(allBlockIds.get(4));

        assertThat(helper.findTransactionsForArtifact(artifactId, blockBeforeStartOfTransactions).getResult()).isEmpty();
        assertThat(helper.findTransactionsForArtifact(artifactId, ROOT_BLOCK).getResult()).isEmpty();

        //more tricky in the middle....;
        var firstTxResult = helper.findTransactionsForArtifact(artifactId, tx1Block);
        assertThat(firstTxResult.getLatestBlockId()).isEqualTo(tx1Block);
        assertThat(firstTxResult.getResult()).hasSize(1);
        assertThat(firstTxResult.getResult().get(0).getTransactionId()).isEqualTo(transaction1);

        //more tricky in the middle....;
        var firstThreeResult = helper.findTransactionsForArtifact(artifactId, tx3Block);
        assertThat(firstThreeResult.getLatestBlockId()).isEqualTo(tx3Block);
        assertThat(firstThreeResult.getResult()).hasSize(3);

        assertThat(firstThreeResult.getResult().get(0).getTransactionId()).isEqualTo(transaction1);
        assertThat(firstThreeResult.getResult().get(1).getTransactionId()).isEqualTo(transaction2);
        assertThat(firstThreeResult.getResult().get(2).getTransactionId()).isEqualTo(transaction3);
    }

    private void awaitCanonization(UUID transactionId) {
        await("find_transaction")
            .pollDelay(Duration.TWO_HUNDRED_MILLISECONDS)
            .pollInterval(Duration.TWO_HUNDRED_MILLISECONDS)
            .atMost(Duration.TEN_SECONDS).until(() -> blockchain.findTransactionById(transactionId).isPresent());
    }

    UUID createTransaction(UUID artifactId, UUID transactionId, UUID previousTransactionId) throws Exception {
        var transaction = BlockchainUtils.transactionCertificateBuilder()
            .artifactType(PAYEE_TYPE_ID)
            .artifactId(artifactId)
            .transactionType(PAYEE_CREATED.getId())
            .transactionId(transactionId)
            .previousTransactionId(previousTransactionId)
            .withFields()
            .emit();
        CompletableFuture<TransactionStatus> result = blockchain.submit(transaction);
        result.get(10, SECONDS);     //waits for the block creation
        return transactionId;
    }
}
