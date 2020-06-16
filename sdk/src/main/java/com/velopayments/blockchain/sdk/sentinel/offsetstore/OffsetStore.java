package com.velopayments.blockchain.sdk.sentinel.offsetstore;

import java.util.UUID;

/**
 * Interface for keeping track of whether Blocks have been processed
 */
public interface OffsetStore {

    BlockOffset initialize();

    boolean isBlockProcessed(long blockHeight);

    void recordBlock(UUID blockId, long blockHeight);
}
