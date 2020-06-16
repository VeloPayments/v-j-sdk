package com.velopayments.blockchain.sdk;

import com.google.common.collect.Iterables;
import com.velopayments.blockchain.cert.CertificateBuilder;
import com.velopayments.blockchain.sdk.sentinel.SentinelRegistry;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;

import static com.velopayments.blockchain.sdk.BlockchainUtils.INITIAL_TRANSACTION_UUID;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class SentinelContainerTest {

    private static final UUID DUMMY_ARTIFACT_TYPE = UUID.randomUUID();
    private static final UUID DUMMY_TRANSACTION_TYPE = UUID.randomUUID();

    SentinelContainer sentinelContainer;
    BlockchainOperations blockchain;

    @Before
    public void setup() {
        this.blockchain = mock(BlockchainOperations.class);

        this.sentinelContainer = new SentinelContainer(blockchain);
    }

    //FIXME: preserving tests
    @Ignore @Test
    public void continerIsNotifiedOfNewBlocks() throws Exception {
        List<UUID> blockIds = new Vector<>();
        BlockCreationListener listener = new BlockCreationListener() {
            @Override
            public Optional<UUID> getLatestNotifiedBlockId() {
                if (blockIds.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(Iterables.getLast(blockIds));
            }

            @Override
            public void blockCreated(UUID latestBlockId) {
                blockIds.add(latestBlockId);
            }
        };
        sentinelContainer.registerBlockCreationListener(listener);

        assertEquals(0, blockIds.size());
        blockchain.submit(dummyTransaction(randomUUID()).emit()).get();

        assertEquals(1, blockIds.size());

        //check that we don't have any duplicate notifications
        assertEquals(1, blockIds.size());


        blockchain.submit(dummyTransaction(randomUUID()).emit()).get();
        blockchain.submit(dummyTransaction(randomUUID()).emit()).get();

        assertEquals(3, blockIds.size());
    }

    private CertificateBuilder dummyTransaction(UUID transactionId) {
        return transactionOfType(transactionId, DUMMY_TRANSACTION_TYPE);
    }

    private CertificateBuilder transactionOfType(UUID transactionId, UUID transactionType) {
        return transactionOfType(transactionId, transactionType, randomUUID(), INITIAL_TRANSACTION_UUID);
    }

    private CertificateBuilder transactionOfType(UUID transactionId, UUID transactionType, UUID artifactId, UUID previousTransactionId) {
        return BlockchainUtils.transactionCertificateBuilder()
            .transactionId(transactionId)
            .previousTransactionId(previousTransactionId)
            .transactionType(transactionType)
            .artifactId(artifactId)
            .artifactType(DUMMY_ARTIFACT_TYPE)
            .timestamp(System.currentTimeMillis())
            .previousState(0)
            .newState(1)
            .withFields();
    }

}
