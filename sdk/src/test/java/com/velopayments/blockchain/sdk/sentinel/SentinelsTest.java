package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.RemoteBlockchain;
import com.velopayments.blockchain.sdk.TestUtils;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.metadata.CoreMetadata;
import com.velopayments.blockchain.sdk.sentinel.criteria.ArtifactIdAndState;
import com.velopayments.blockchain.sdk.sentinel.criteria.Criteria;
import com.velopayments.blockchain.sdk.sentinel.criteria.InvalidCriteriaException;
import com.velopayments.blockchain.sdk.sentinel.offsetstore.BlockOffset;
import com.velopayments.blockchain.sdk.sentinel.offsetstore.OffsetStore;
import org.junit.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.velopayments.blockchain.cert.CertificateType.ROOT_BLOCK;
import static com.velopayments.blockchain.sdk.sentinel.PayeeMetadata.*;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public class SentinelsTest {

    @Rule
    public GenericContainer<?> agentdContainer = TestUtils.agentdContainer();

    @Rule
    public GenericContainer<MockServerContainer> vaultContainer = TestUtils.vaultContainer();

    RemoteBlockchain blockchain;
    SentinelRegistry registry;
    SentinelPollerTask pollerTask;

    @Before
    public void setup() throws Exception {
        blockchain = TestUtils.createTestBlockchain(agentdContainer, vaultContainer);
        blockchain.start();
        registry = new SentinelRegistry();
        fail("fixme"); //FIXME: sentinels
//        var pollerTaskLog = new FileSystemOffsetStore(folder.newFolder().toPath().resolve("sentinel.log"));
//        pollerTask = new SentinelPollerTask(blockchain, pollerTaskLog, registry);
    }

    @After
    public void teardown() throws Exception {
        if (registry != null) registry.close();
        if (blockchain != null) blockchain.close();
    }

    @Test
    public void baseCase() throws Exception {
        createTransaction();

        var criteria = Criteria.withLatestBlockId(latestBlockId());     //simply listening for new blocks - no complicated criteria
        pollerTask.processLatestBlocks();       //this simulates the background sentinel task but calling it synchronously to make testing easier;

        TestSentinel testSentinel = new TestSentinel();
        Map<String, Object> settings = new HashMap<>();
        var registrationHandle = registry.register("a", testSentinel, settings, blockchain);
        assertThat(registrationHandle.getSentinel()).isEqualTo(testSentinel);

        pollerTask.processLatestBlocks();
        assertThat(testSentinel.called).isFalse();    //nothing should have been triggered yet;

        createTransaction();
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.called).isTrue();
        assertThat(testSentinel.latestBlockId).isEqualTo(latestBlockId());
        assertThat(testSentinel.criteria).isEqualTo(criteria);
    }

    @Test(expected = InvalidCriteriaException.class)
    public void triggerValidation() {
        Map<String, Object> settings = new HashMap<>();
        registry.register("a", new TestSentinel(), settings, blockchain);
    }

    @Test
    public void artifactIdAndStateTrigger_NotifiedWhenArtifactChangesState() throws Exception {
        var artifactId = randomUUID();

        Criteria.builder()
            .latestBlockId(latestBlockId())
            .artifactIdAndState(new ArtifactIdAndState(artifactId, STATE_NEW.getValue()))
            .build();

        Map<String, Object> settings = new HashMap<>();
        TestSentinel testSentinel = new TestSentinel();
        registry.register("a", testSentinel, settings, blockchain);

        createTransaction(artifactId, PAYEE_CREATED.getId(), "Bob", PAYEE_TYPE_ID, STATE_NEW.getValue());
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(0); //state is still 'NEW' (100), as per the criteria;

        createTransaction(artifactId, PAYEE_CREATED.getId(), "Bob", PAYEE_TYPE_ID, STATE_ACTIVE.getValue());
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1);          //now that the state is 'ACTIVE' we should have a notification;
    }

    @Test
    public void artifactIdAndStateTrigger_NotifiedOnAnyChange() throws Exception {
        var artifactId = randomUUID();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .artifactId(artifactId)
            .build();

        Map<String, Object> settings = new HashMap<>();
        TestSentinel testSentinel = new TestSentinel();
        registry.register("a", testSentinel, settings, blockchain);

        createTransaction();
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(0);

        createTransaction(artifactId);
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1);
    }

    @Test
    public void artifactIdAndStateTrigger_NotifiedOnAnyChangeByType() throws Exception {
        TestSentinel testSentinel = new TestSentinel();
        Map<String, Object> settings = new HashMap<>();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .artifactTypeId(PAYEE_TYPE_ID)
            .build();
        registry.register("a", testSentinel, settings, blockchain);

        createTransaction(randomUUID(), PAYEE_CREATED.getId(), "Bob", randomUUID(), STATE_NEW.getValue());
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(0);

        createTransaction(randomUUID(), PAYEE_CREATED.getId(), "Bob", PAYEE_TYPE_ID, STATE_NEW.getValue());
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1);
    }

    @Test
   public void artifactIdAndStateTrigger_NotifiedOnAnyNewChangesByType() throws Exception {
        TestSentinel testSentinel = new TestSentinel();
        Map<String, Object> settings = new HashMap<>();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .transactionType(PAYEE_CREATED.getId())
            .build();
        registry.register("a", testSentinel, settings, blockchain);

        createTransaction();
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(0);

        createTransaction(randomUUID());
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1);
    }

    @Test
    public void offsetstoreSavesState() throws Exception {
        //try processing the same block twice and make sure we only get a single notification
        TestSentinel testSentinel = new TestSentinel();
        Map<String, Object> settings = new HashMap<>();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .build();
        var registration = registry.register("a", testSentinel, settings, blockchain);

        pollerTask.processLatestBlocks();

        createTransaction();
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1);

        pollerTask.processLatestBlocks();    //high water mark is protecting us;
        assertThat(testSentinel.calls.get()).isEqualTo(1);


        //even if we mess up the high water mark then the offset store will protect us;
        registration.cancel();

        OffsetStore offsetStore = mock(OffsetStore.class);
        BlockOffset blockOffset = new BlockOffset();
        blockOffset.setBlockId(ROOT_BLOCK);
        blockOffset.setBlockHeight(0L);
        when(offsetStore.initialize()).thenReturn(blockOffset);
        pollerTask = new SentinelPollerTask(blockchain, offsetStore, registry);
        assertThat(pollerTask.currentHighWaterMark()).isEqualTo(ROOT_BLOCK);

        Criteria.builder()
            .latestBlockId(latestBlockId())
            .build();
        registry.register("a", testSentinel, settings, blockchain);

        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1);
    }

    @Test
    public void registrationsCanBeCancelled() throws Exception {
        TestSentinel testSentinel = new TestSentinel();
        Map<String, Object> settings = new HashMap<>();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .build();
        var registration = registry.register("a", testSentinel, settings, blockchain);

        createTransaction();
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1);

        registration.cancel();

        createTransaction();
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1);
    }


    @Test
    public void returningEmptyCriteriaMeansSentinelWillNotBeCalledAgain() throws Exception {
        TestSentinel testSentinel = new TestSentinel();
        Map<String, Object> settings = new HashMap<>();
        testSentinel.criteriaToReturn = Optional.empty();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .build();
        registry.register("a", testSentinel, settings, blockchain);

        createTransaction();
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1);

        createTransaction();
        pollerTask.processLatestBlocks();
        assertThat(testSentinel.calls.get()).isEqualTo(1); //still 1 - hasn't been called again;
    }

    @Test
    public void sentinelFailureWillNotBeRepeatedAndIsIsolated() throws Exception {
        TestSentinel npeSentinel = new TestSentinel();
        npeSentinel.throwException = true;
        Map<String, Object> settings = new HashMap<>();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .build();
        registry.register("a", npeSentinel, settings, blockchain);

        TestSentinel testSentinel = new TestSentinel();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .build();
        registry.register("a", testSentinel, settings, blockchain);

        createTransaction();
        pollerTask.processLatestBlocks();
        assertThat(npeSentinel.calls.get()).isEqualTo(1);
        assertThat(testSentinel.calls.get()).isEqualTo(1);

        pollerTask.processLatestBlocks();    //noone should be called again - there is no retry logic;
        assertThat(npeSentinel.calls.get()).isEqualTo(1);
        assertThat(testSentinel.calls.get()).isEqualTo(1);

        createTransaction(); //when we create another transaction then they bot get called again;
        pollerTask.processLatestBlocks();
        assertThat(npeSentinel.calls.get()).isEqualTo(2);
        assertThat(testSentinel.calls.get()).isEqualTo(2);
    }

    @Test(expected = SentinelRegistryException.class)
    public void registerNewSentinelAfterRegistryClosed() throws Exception {
        registry.close();
        TestSentinel testSentinel = new TestSentinel();
        Map<String, Object> settings = new HashMap<>();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .build();
        registry.register("a", testSentinel, settings, blockchain);
    }

    @Test
    public void sentinelsTaskStartedAutomatically() throws Exception {
        //register via blockchain method, because we don't want to be in control of checking for latest blocks.  We want the thread pool to do it
        TestSentinel testSentinel = new TestSentinel();
        Map<String, Object> settings = new HashMap<>();
        Criteria.builder()
            .latestBlockId(latestBlockId())
            .build();
        registry.register("a", testSentinel, settings, blockchain);

        createTransaction();
        await().atMost(5, SECONDS).untilAsserted(() ->
            assertThat(testSentinel.called).isTrue());
    }

    public static class TestSentinel implements Sentinel {
        volatile boolean called = false;
        volatile UUID latestBlockId;
        volatile Criteria criteria;

        AtomicInteger calls = new AtomicInteger(0);

        Optional<Criteria> criteriaToReturn;
        boolean throwException = false;

        TestSentinel() {
        }

        @Override
        public ConfigDef config() {
            return null; //FIXME sentinel
        }

        @Override
        public Criteria start(Map<String, Object> settings, BlockchainOperations blockchain) {
            throw new UnsupportedOperationException("fixme"); //FIXME sentinel
        }

        @Override
        public Optional<Criteria> notify(UUID latestBlockId, Criteria criteria) {
            this.called = true;
            this.latestBlockId = latestBlockId;
            this.criteria = criteria;
            this.calls.incrementAndGet();

            if (throwException) {
                throw new RuntimeException("This is a deliberate exception");
            }

            if (criteriaToReturn != null) {
                return criteriaToReturn;
            }
            return criteria.withBlockId(latestBlockId);
        }
    }


    private UUID createTransaction(UUID artifactId) throws Exception {
        return createTransaction(artifactId, PAYEE_CREATED.getId(), "Bob", PAYEE_TYPE_ID, STATE_NEW.getValue());
    }

    private UUID createTransaction() throws Exception {
        return createTransaction(randomUUID(), PAYEE_CREATED.getId(), "Bob", PAYEE_TYPE_ID, STATE_NEW.getValue());
    }

    private UUID createTransaction(UUID artifactId, UUID transactionType, String displayName, UUID artifactTypeId, Integer state) throws Exception {
        var transactionId = randomUUID();
        var transaction = BlockchainUtils.transactionCertificateBuilder()
            .artifactType(artifactTypeId)
            .artifactId(artifactId)
            .transactionType(transactionType)
            .transactionId(transactionId)
            .previousTransactionId(BlockchainUtils.INITIAL_TRANSACTION_UUID)
            .newState(state)
            .withFields()
            .addString(CoreMetadata.DISPLAY_NAME.getId(), displayName)
            .emit();
        blockchain.submit(transaction).get(10, SECONDS);
        return transactionId;
    }

    private UUID latestBlockId() {
        return blockchain.getLatestBlockId();
    }
}
