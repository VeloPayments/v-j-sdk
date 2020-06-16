package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.sdk.BlockReader;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.sentinel.criteria.Criteria;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.velopayments.blockchain.cert.CertificateType.ROOT_BLOCK;

public class RegistrationHandleImpl implements RegistrationHandle {

    @Getter
    private final String name;

    @Getter
    private final Sentinel sentinel;

    @Getter
    private final SentinelRegistry sentinelNotificationRegistry;

    @Getter
    private final Map<String, Object> settings;

    @Getter
    private final BlockchainOperations blockchain;

    private RegistrationStatus status;

    private Criteria criteria;

    public RegistrationHandleImpl(String name,
                                  Sentinel sentinel,
                                  SentinelRegistry sentinelNotificationRegistry,
                                  Map<String, Object> settings,
                                  BlockchainOperations blockchain,
                                  RegistrationStatus status) {
        if (Objects.requireNonNull(name).length() == 0) {
            throw new IllegalArgumentException("Sentinel cannot be registered with an empty string: " + sentinel.getClass());
        }
        this.name = name;
        this.sentinel = Objects.requireNonNull(sentinel);
        this.sentinelNotificationRegistry = Objects.requireNonNull(sentinelNotificationRegistry);
        this.settings = Objects.requireNonNull(settings);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.status = Objects.requireNonNull(status);
        this.criteria = Criteria.builder().latestBlockId(ROOT_BLOCK).build();
    }

    public static RegistrationHandleImplBuilder builder() {
        return new RegistrationHandleImplBuilder();
    }

    @Override
    public synchronized void cancel() {
        this.sentinelNotificationRegistry.remove(this.name);
    }

    public synchronized boolean isActive() {
        return this.status == RegistrationStatus.Active;
    }

    public synchronized Criteria getCriteria() {
        return this.criteria;
    }

    public synchronized void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    public synchronized RegistrationStatus getRegistrationStatus() {
        return this.status;
    }

    public synchronized void setRegistrationStatus(RegistrationStatus status) {
        this.status = status;
    }

    public boolean isTriggeredBy(BlockReader blockReader) {
        //check for changes to a particular artifact
        if (this.criteria.getArtifactId() != null) {
            for (TransactionReader tx : blockReader.getTransactions()) {
                return this.criteria.getArtifactId().equals(tx.getArtifactId());
            }
        }

        //check for changes based on artifact type id
        if (this.criteria.getArtifactTypeId() != null) {
            for (TransactionReader tx : blockReader.getTransactions()) {
                return this.criteria.getArtifactTypeId().equals(tx.getArtifactType());
            }
        }

        //check for changes based on a transaction type
        if (this.criteria.getTransactionType() != null) {
            for (TransactionReader tx : blockReader.getTransactions()) {
                return this.criteria.getTransactionType().equals(tx.getTransactionType());
            }
        }

        if (this.criteria.getArtifactIdAndState() != null) {
            //get the latest transaction for the artifact and compare the state to the criteria state
            for (TransactionReader tx : blockReader.getTransactions()) {
                if (this.criteria.getArtifactIdAndState().getArtifactId().equals(tx.getArtifactId())) {
                    //if the transaction contains a different tate for this artifact, let's trigger.
                    return this.blockchain.findTransactionById(tx.getTransactionId())
                        .flatMap(r -> Optional.ofNullable(r.getNewArtifactState()))
                        .filter(s -> !s.equals(this.criteria.getArtifactIdAndState().getState()))
                        .isPresent();
                }
            }
            return false;   //didn't find a match or state change
        }

        //because latestBlockId is mandatory in the criteria and the offset log protects us from duplicate notifications, we must return true here
        return true;
    }

    public static class RegistrationHandleImplBuilder {
        private String name;
        private Sentinel sentinel;
        private SentinelRegistry sentinelNotificationRegistry;
        private Map<String, Object> settings;
        private BlockchainOperations blockchain;
        private RegistrationStatus status;
        private Criteria criteria;

        RegistrationHandleImplBuilder() {
        }

        public RegistrationHandleImplBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RegistrationHandleImplBuilder sentinel(Sentinel sentinel) {
            this.sentinel = sentinel;
            return this;
        }

        public RegistrationHandleImplBuilder sentinelNotificationRegistry(SentinelRegistry sentinelNotificationRegistry) {
            this.sentinelNotificationRegistry = sentinelNotificationRegistry;
            return this;
        }

        public RegistrationHandleImplBuilder settings(Map<String, Object> settings) {
            this.settings = settings;
            return this;
        }

        public RegistrationHandleImplBuilder blockchain(BlockchainOperations blockchain) {
            this.blockchain = blockchain;
            return this;
        }

        public RegistrationHandleImplBuilder status(RegistrationStatus status) {
            this.status = status;
            return this;
        }

        public RegistrationHandleImplBuilder criteria(Criteria criteria) {
            this.criteria = criteria;
            return this;
        }

        public RegistrationHandleImpl build() {
            return new RegistrationHandleImpl(name, sentinel, sentinelNotificationRegistry, settings, blockchain, status);
        }

        public String toString() {
            return "RegistrationHandleImpl.RegistrationHandleImplBuilder(name=" + this.name + ", sentinel=" + this.sentinel + ", sentinelNotificationRegistry=" + this.sentinelNotificationRegistry + ", settings=" + this.settings + ", blockchain=" + this.blockchain + ", status=" + this.status +")";
        }
    }
}
