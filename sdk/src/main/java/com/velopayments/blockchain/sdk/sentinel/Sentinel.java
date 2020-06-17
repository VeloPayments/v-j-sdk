package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.sentinel.criteria.Criteria;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sentinels are callbacks that work with the {@link SentinelRegistry} to be invoked when their criteria becomes invalidated
 *
 * Note: {@link Sentinel}s are only called once for a given {@link Criteria}
 * Future callbacks are scheuled via the return value from the notify method
 */
public interface Sentinel {

    ConfigDef config();

    Criteria start(Map<String, Object> settings, BlockchainOperations blockchain);

    /**
     * Called to notify the Sentinel that the criteria has been triggered
     * @return further criteria to be used
     */
    Optional<Criteria> notify(UUID latestBlockId, Criteria criteria);
}
