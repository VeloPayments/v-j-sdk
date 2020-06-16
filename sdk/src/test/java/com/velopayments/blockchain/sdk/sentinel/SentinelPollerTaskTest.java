package com.velopayments.blockchain.sdk.sentinel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.CertificateBuilder;
import com.velopayments.blockchain.cert.CertificateType;
import com.velopayments.blockchain.cert.Field;
import com.velopayments.blockchain.sdk.BlockReader;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.sentinel.offsetstore.FileSystemOffsetStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.velopayments.blockchain.cert.CertificateType.ROOT_BLOCK;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class SentinelPollerTaskTest {

    private SentinelPollerTask sentinelPollerTask;

    private BlockchainOperations blockchain;

    private SentinelRegistry sentinelRegistry;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {

        blockchain = mock(BlockchainOperations.class);

        sentinelRegistry = mock(SentinelRegistry.class);
        sentinelPollerTask = new SentinelPollerTask(blockchain, new FileSystemOffsetStore(tempFolder.newFile().toPath()), sentinelRegistry);
    }

    @Test
    public void sentinelsAreInvokedOnlyOnce() {

        // given a block with two transactions
        UUID tx1Id = randomUUID();
        UUID tx1Type = randomUUID();

        UUID tx2Id = randomUUID();
        UUID tx2Type = randomUUID();

        stubBlockWithTransactions(tx1Id, tx1Type, tx2Id, tx2Type);

        // when the poller executes, with sentinel registry throwing an exception
        doThrow(new RuntimeException("pow")).when(sentinelRegistry).notifyBlock(any());
        sentinelPollerTask.processLatestBlocks();

        // then both sentinels are invoked for the proper transactions
        var blockArg = ArgumentCaptor.forClass(BlockReader.class);
        verify(sentinelRegistry, times(1)).notifyBlock(blockArg.capture());
        assertThat(blockArg.getValue().getTransactions().size()).isEqualTo(2);


        // when the poller executes again, the sentinel registry is not invoked again
        sentinelPollerTask.processLatestBlocks();
        verify(sentinelRegistry, times(1)).notifyBlock(any());

    }

    private TransactionReader dummyTransaction(UUID transactionId, UUID previousTransactionId, UUID transactionType) {
        Certificate cert = BlockchainUtils.transactionCertificateBuilder()
            .transactionId(transactionId)
            .previousTransactionId(previousTransactionId)
            .transactionType(transactionType)
            .artifactId(randomUUID())
            .artifactType(randomUUID())
            .timestamp(System.currentTimeMillis())
            .previousState(0)
            .newState(1)
            .withFields()
            .emit();
        return new TransactionReader(cert);
    }

    private UUID stubBlockWithTransactions(UUID tx1Id, UUID tx1Type, UUID tx2Id, UUID tx2Type) {

        UUID blockId = randomUUID();

        TransactionReader tx1 = dummyTransaction(tx1Id, ROOT_BLOCK, tx1Type);
        when(blockchain.findTransactionById(tx1Id)).thenReturn(Optional.of(tx1));

        TransactionReader tx2 = dummyTransaction(tx2Id, tx1Id, tx2Type);
        when(blockchain.findTransactionById(tx2Id)).thenReturn(Optional.of(tx2));

        Certificate block = CertificateBuilder.createCertificateBuilder(CertificateType.BLOCK)
            .addUUID(Field.BLOCK_UUID, blockId)
            .addLong(Field.BLOCK_HEIGHT, 1L)
            .addByteArray(Field.WRAPPED_TRANSACTION_TUPLE, tx1.getCertificate().toByteArray())
            .addByteArray(Field.WRAPPED_TRANSACTION_TUPLE, tx1.getCertificate().toByteArray())
            .emit();

        when(blockchain.findNextBlockId(CertificateType.ROOT_BLOCK)).thenReturn(Optional.of(blockId));
        when(blockchain.findNextBlockId(blockId)).thenReturn(Optional.empty());
        when(blockchain.findBlockById(blockId)).thenReturn(Optional.of(new BlockReader(block)));


        when(blockchain.findAllBlocksAfter(ROOT_BLOCK)).thenReturn(Stream.of(new BlockReader(block)));
        when(blockchain.findAllBlocksAfter(blockId)).thenReturn(Stream.empty());

        return blockId;
    }
}
