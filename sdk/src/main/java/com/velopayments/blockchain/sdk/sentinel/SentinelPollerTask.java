package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.cert.CertificateType;
import com.velopayments.blockchain.sdk.BlockReader;
import com.velopayments.blockchain.sdk.BlockchainException;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.sentinel.offsetstore.OffsetStore;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Slf4j
public class SentinelPollerTask {

    private final BlockchainOperations blockchain;
    private final OffsetStore offsetStore;
    private final SentinelRegistry sentinelRegistry;

    private UUID highWaterMark;

    public SentinelPollerTask(BlockchainOperations blockchain, OffsetStore offsetStore, SentinelRegistry sentinelRegistry) {
        this.blockchain = requireNonNull(blockchain);
        this.offsetStore =  requireNonNull(offsetStore);
        this.sentinelRegistry =  requireNonNull(sentinelRegistry);

        // process the last block recorded.  Any transactions that have already been processed will not be reprocessed.
        UUID lastBlockId = offsetStore.initialize().getBlockId();
        if (!CertificateType.ROOT_BLOCK.equals(lastBlockId)) {
            BlockReader blockReader = blockchain.findBlockById(lastBlockId)
                .orElseThrow(() -> new BlockchainException("Couldn't find block: " + lastBlockId));
            processBlock(blockReader);
        }
        else {
            highWaterMark = CertificateType.ROOT_BLOCK;
        }
    }

    UUID currentHighWaterMark(){
        return highWaterMark;
    }

    public void processLatestBlocks() {
        blockchain.findAllBlocksAfter(highWaterMark).forEach(this::processBlock);
    }

    private void processBlock(BlockReader blockReader) {
        UUID blockId = blockReader.getBlockId();
        long blockHeight = blockReader.getBlockHeight();

        //let the SentinelRegistry know about the new block
        if (!offsetStore.isBlockProcessed(blockHeight)) {
            log.info("notifying block height {} - {}", blockId, blockHeight);
            try {
                sentinelRegistry.notifyBlock(blockReader);
            } catch (RuntimeException e) { // retry logic is out of scope for now
                log.error("Error notifying sentinels of new blocks", e);
            }
            offsetStore.recordBlock(blockId, blockHeight);
        }
        highWaterMark = blockId;
    }
}
