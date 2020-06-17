package com.velopayments.blockchain.sdk.metadata;


import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.RemoteBlockchain;
import com.velopayments.blockchain.sdk.TestUtils;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.velopayments.blockchain.cert.Field.PREVIOUS_CERTIFICATE_ID;
import static com.velopayments.blockchain.sdk.metadata.CoreMetadata.CORE_METADATA_TYPE_ID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ArtifactTypeMetadataBlockchainTest {

    private static final long TO = 1000L;

    @ClassRule
    public static GenericContainer<?> agentdContainer = TestUtils.agentdContainer();

    @ClassRule
    public static GenericContainer<MockServerContainer> vaultContainer = TestUtils.vaultContainer();

    private static RemoteBlockchain blockchain;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EntityKeys entityKeys = EntityTool.generate("TEST");
    private ArtifactTypeMetadataAccessBlockchain metadataAccess;

    @BeforeClass
    public static void start() throws Exception {
        blockchain = TestUtils.createTestBlockchain(agentdContainer, vaultContainer);
        blockchain.start();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        blockchain.close();
    }

    @Before
    public void setUp() {
        this.metadataAccess = new ArtifactTypeMetadataAccessBlockchain(blockchain);
    }

    @Test @Ignore//FIXME: timing ... this test assumes the blockchain in a clean state before the test
    public void writeMetadataToBlockchain() throws Exception {
        UUID artifactTypeId = UUID.randomUUID();
        store(new ArtifactTypeMetadataBuilder(artifactTypeId, "BAZ")
            .addTransactionType(TransactionType.builder()
                .id(UUID.randomUUID())
                .name("CREATE_BAZ")
                .build()));

        assertStoredArtifactType(artifactTypeId, "CREATE_BAZ");

        //clear the cache and assert that again on round-tripped data
        metadataAccess.getCache().clear();

        assertStoredArtifactType(artifactTypeId, "CREATE_BAZ");
    }

    public void assertStoredArtifactType(UUID artifactTypeId, String typeName) {
        assertThat(metadataAccess.findByArtifactTypeId(artifactTypeId)).hasValueSatisfying( metadata -> {
            assertThat(DefaultGroovyMethods.getAt(metadata.getTransactionTypes().values(), 0).getName()).isEqualTo(typeName);

            //test that the core metadata was linked as a supertype
            assertThat(metadata.findFieldById(CoreMetadata.DISPLAY_NAME.getId()).map(FieldMetadata::getName)).hasValue("DISPLAY_NAME");
            assertThat(metadata.findFieldById(Field.CERTIFICATE_ID)).map(FieldMetadata::getName).hasValue("CERTIFICATE_ID");
        });
    }

    @Test  @Ignore //FIXME: timing ... this test assumes the blockchain in a clean state before the test
    public void coreMedata_MetadataNotPresent() throws Exception {
        //assert that core metadata has not yet been written
        assertThat(blockchain.findLastTransactionIdForArtifactById(CORE_METADATA_TYPE_ID)).isNotEmpty();
        writeCoreMetadata();

        assertThat(blockchain.findLastTransactionIdForArtifactById(CORE_METADATA_TYPE_ID)
            .flatMap(blockchain::findTransactionById)).hasValueSatisfying(coreTxCertReader -> {
                UUID coreTxCertPrevTransactionId = coreTxCertReader.getFirst(PREVIOUS_CERTIFICATE_ID).asUUID();
                assertThat(coreTxCertPrevTransactionId).isEqualTo(BlockchainUtils.INITIAL_TRANSACTION_UUID);
            });
    }

    @Test  @Ignore //FIXME: timing ... this test assumes the blockchain in a clean state before the test
    public void coreMedata_outdatedMetadataDefinitionIsOverwritten() throws Exception {
        //write some rubbish under the core metadata type id
        store(new ArtifactTypeMetadataBuilder(CORE_METADATA_TYPE_ID, "CORE_METADATA"));

        //read back the dodgy core metadata
        assertThat(metadataAccess.findByArtifactTypeId(CORE_METADATA_TYPE_ID)).hasValueSatisfying(metadata -> {
            assertThat(metadata.getArtifactTypeId()).isEqualTo(CORE_METADATA_TYPE_ID);
            assertThat(metadata.getArtifactTypeName()).isEqualTo("CORE_METADATA");
            assertThat(metadata.getFields()).isEmpty();
        });

        //now write back the proper core metadata
        writeCoreMetadata();

        assertThat(metadataAccess.findByArtifactTypeId(CORE_METADATA_TYPE_ID)).hasValueSatisfying(metadata -> {
            assertThat(metadata.getFields()).isNotEmpty();
        });
    }


    @Test
    public void coreMedata_alreadyUpToDate() throws Exception {
        writeCoreMetadata();

        UUID lastTxId1 = blockchain.findLastTransactionIdForArtifactById(CORE_METADATA_TYPE_ID).get();

        //do the check again - should be no action and the last certificate id of the
        assertThat(metadataAccess.ensureCoreMetadataIsUpToDate(entityKeys).get(TO, SECONDS))
            .isEqualTo(TransactionStatus.SUCCEEDED);

        UUID lastTxId2 = blockchain.findLastTransactionIdForArtifactById(CORE_METADATA_TYPE_ID).get();

        //assert that no new transactions were written
        assertThat(lastTxId1).isEqualTo(lastTxId2);
    }

    @Test @Ignore //FIXME: timing ... this test assumes the blockchain in a clean state before the test
    public void duplicateMedataIsNotWritten() throws Exception {
        //1. write the metadata - check it is in the blockchain
        //2. write a changed version of the metadata - check that a new version is written to the blockchain
        //3. write unchanged metadata - no new transaction should be written
        UUID artifactTypeId = UUID.randomUUID();
        UUID txTypeId = UUID.fromString("286a8898-fd00-4a87-a02e-a99b84e085ff");

        //1. write the metadata - check it is in the blockchain
        store(new ArtifactTypeMetadataBuilder(artifactTypeId, "FOO")
            .addTransactionType(TransactionType.builder()
                .id(txTypeId)
                .name("CREATE_FOO")
                .build()));
        UUID lastTxId1 = blockchain.findLastTransactionIdForArtifactById(artifactTypeId).orElseThrow();

        //2. write a changed version of the metadata - check that a new version is written to the blockchain
        store(new ArtifactTypeMetadataBuilder(artifactTypeId, "FOO")
            .addTransactionType(TransactionType.builder()
                .id(txTypeId)
                .name("CREATE_FOO_REDEFINED")
                .build()));
        UUID lastTxId2 = blockchain.findLastTransactionIdForArtifactById(artifactTypeId).orElseThrow();
        assertThat(lastTxId1).isNotEqualTo(lastTxId2);


        //3. write unchanged metadata - no new transaction should be written
        store(new ArtifactTypeMetadataBuilder(artifactTypeId, "FOO")
            .addTransactionType(TransactionType.builder()
                .id(txTypeId).name("CREATE_FOO_REDEFINED")
                .build()));
        UUID lastTxId3 = blockchain.findLastTransactionIdForArtifactById(artifactTypeId).orElseThrow();
        assertThat(lastTxId2).isEqualTo(lastTxId3);
    }

    @Test @Ignore //FIXME: timing ... this test assumes the blockchain in a clean state before the test
    public void overwritingWithNewMetadata() throws Exception {
        UUID artifactTypeId = UUID.randomUUID();
        store(new ArtifactTypeMetadataBuilder(artifactTypeId, "PAYEE")
            .addTransactionType(TransactionType.builder()
                .id(UUID.randomUUID())
                .name("CREATE_PAYEE")
                .build()));

        store(new ArtifactTypeMetadataBuilder(artifactTypeId, "PAYEE")
            .addTransactionType(TransactionType.builder()
                .id(UUID.randomUUID())
                .name("CREATE_PAYEE_REDEFINED")
                .build()));

        //read it back from the blockchain and verify that we get the redefined (over-written) metadata not the original
        assertThat(metadataAccess.findByArtifactTypeId(artifactTypeId)).hasValueSatisfying(metadata -> {
            Collection<TransactionType> values = metadata.getTransactionTypes().values();
            assertThat(DefaultGroovyMethods.getAt(values, 0).getName()).isEqualTo("CREATE_PAYEE_REDEFINED");
        });

        //test that we have both artifact type transactions in the blockchain
        TransactionReader cert2Reader = blockchain.findLastTransactionIdForArtifactById(artifactTypeId)
            .flatMap(id -> blockchain.findTransactionById(id))
            .orElseThrow();

        UUID cert2PrevTransactionId = cert2Reader.getFirst(PREVIOUS_CERTIFICATE_ID).asUUID();
        assertThat(cert2PrevTransactionId).isNotEqualTo(BlockchainUtils.INITIAL_TRANSACTION_UUID);

        TransactionReader cert1Reader = blockchain.findTransactionById(cert2PrevTransactionId).orElseThrow();
        UUID cert1PrevTransactionId = cert1Reader.getFirst(PREVIOUS_CERTIFICATE_ID).asUUID();
        assertThat(cert1PrevTransactionId).isEqualTo(BlockchainUtils.INITIAL_TRANSACTION_UUID);
    }

    @Test
    public void caching() throws Exception {
        //check the cache is read-through
        assertThat(metadataAccess.getCache()).isEmpty();

        writeCoreMetadata();

        assertThat(metadataAccess.getCache()).isNotEmpty();

        //check the cache is write-through
        UUID artifactTypeId = UUID.randomUUID();
        metadataAccess.store(new ArtifactTypeMetadataBuilder(artifactTypeId, "BAR"), entityKeys).get(TO, SECONDS);
        ArtifactTypeMetadata metadata = metadataAccess.getCache().get(artifactTypeId);
        assertThat(metadata.getArtifactTypeName()).isEqualTo("BAR");

        //check updating the cache upon write
        metadataAccess.store(new ArtifactTypeMetadataBuilder(artifactTypeId, "BAR_UPDATED"), entityKeys).get(TO, SECONDS);
        metadata = metadataAccess.getCache().get(artifactTypeId);
        assertThat(metadata.getArtifactTypeName()).isEqualTo("BAR_UPDATED");
    }

    @Test @Ignore //FIXME: timing ... this test assumes the blockchain in a clean state before the test
    public void listArtifactTypes() throws Exception {
        List.of("FOO", "BAR").forEach(name -> {
            store(new ArtifactTypeMetadataBuilder(UUID.randomUUID(), name));
        });
        metadataAccess.getCache().clear();

        Collection<ArtifactTypeMetadata> types = metadataAccess.listArtifactTypes();
        assertThat(types).isNotEmpty();
        assertThat(types.stream().map(ArtifactTypeMetadata::getArtifactTypeName))
            .contains("FOO", "BAR");
        assertThat(types.stream().map(ArtifactTypeMetadata::getArtifactTypeId))
            .contains(CORE_METADATA_TYPE_ID);
    }

    public void writeCoreMetadata() throws Exception {
        assertThat(metadataAccess.ensureCoreMetadataIsUpToDate(entityKeys).get(TO, SECONDS))
            .isEqualTo(TransactionStatus.SUCCEEDED);
    }

//    private void awaitCanonization(ThrowingRunnable assertion) {
//        await("canonization")
//            .pollDelay(Duration.TWO_HUNDRED_MILLISECONDS)
//            .pollInterval(Duration.TWO_HUNDRED_MILLISECONDS)
//            .atMost(Duration.TEN_SECONDS)
//            .untilAsserted(assertion);
//    }

    public void store(ArtifactTypeMetadataBuilder builder) {
        MetadataHelper.initMetadata(blockchain, entityKeys, builder);
    }
}
