package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.sdk.BlockReader;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.sentinel.criteria.Criteria;
import com.velopayments.blockchain.sdk.sentinel.criteria.InvalidCriteriaException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.velopayments.blockchain.sdk.metadata.ArtifactTypeMetadataBuilder.assertNotNull;
import static com.velopayments.blockchain.sdk.sentinel.RegistrationStatus.Active;

@Slf4j
public class SentinelRegistry {

    private final Map<String, RegistrationHandleImpl> registrations = new HashMap<>();
    private final AtomicBoolean active = new AtomicBoolean(false);

    public SentinelRegistry() {
    }

    /**
     * Register a {@link Sentinel} to receive notifications against a criteria
     *
     * @return a handle that can be used to cancel the registration
     *
     * @throws InvalidCriteriaException if the given criteria is invalid
     * @throws SentinelRegistryException the registry is shutting down
     */
    public RegistrationHandle register(String name,
                                       Sentinel sentinel,
                                       Map<String, Object> settings,
                                       BlockchainOperations blockchain) {
        if (active.get()) {
            throw new SentinelRegistryException("SentinelRegistry is active");
        }

        RegistrationHandleImpl handler = RegistrationHandleImpl.builder()
            .name(name)
            .settings(settings)
            .sentinel(sentinel)
            .sentinelNotificationRegistry(this)
            .blockchain(blockchain)
            .status(Active)
            .build();
        if (registrations.containsKey(name)) {
            throw new SentinelRegistryException("Sentinel already registered with " + name);
        }
        registrations.put(name, handler);

        return handler;
    }


    /**
     * Start all {@link RegistrationHandle}s
     */
    public void start() {
        registrations.values().forEach(handle -> {
            Criteria initial = handle.getSentinel().start(handle.getSettings(), handle.getBlockchain());
            validateCriteria(initial);
            handle.setCriteria(initial);
        });
        active.set(true);
    }

    /**
     * Cancel all {@link RegistrationHandle}s, stop listening for new events
     */
    public void close() {
        active.set(false);
        registrations.values().forEach(RegistrationHandle::cancel);
    }

    public void remove(String registration) {
        registrations.remove(registration);
    }

    public void notifyBlock(BlockReader blockReader) {
        log.trace("notifyBlock: {} - {}   {} registrations", blockReader.getBlockHeight(), blockReader.getBlockId(), registrations.size());
        registrations.values().forEach(h -> {
            // TODO: avoid the cast
            if (h.isActive() && h.isTriggeredBy(blockReader)) {
                try {
                    Sentinel sentinel = h.getSentinel();
                    log.trace("  notifying: {}", sentinel);
                    Optional<Criteria> newCriteria = assertNotNull(sentinel.notify(blockReader.getBlockId(), h.getCriteria()));

                    if (newCriteria.isPresent()) {
                        validateCriteria(newCriteria.get());
                        h.setCriteria(newCriteria.get());
                    } else {
                        h.cancel();
                    }
                }
                catch (RuntimeException e) { // retry logic is out of scope for now
                    log.error("Error notifying sentinel " + h + " of new blocks", e);
                }
            }
        });
    }


    static Criteria validateCriteria(Criteria criteria) throws InvalidCriteriaException {
        if (criteria == null) {
            throw new InvalidCriteriaException("Criteria may not be null");
        }
        return criteria.validate();
    }
}
