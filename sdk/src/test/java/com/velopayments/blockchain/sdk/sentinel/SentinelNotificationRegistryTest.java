package com.velopayments.blockchain.sdk.sentinel;

import com.velopayments.blockchain.sdk.sentinel.criteria.ArtifactIdAndState;
import com.velopayments.blockchain.sdk.sentinel.criteria.Criteria;
import com.velopayments.blockchain.sdk.sentinel.criteria.InvalidCriteriaException;
import org.junit.Test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

public class SentinelNotificationRegistryTest {

    @Test
    public void validate() throws Exception {
        //var registry = new SentinelRegistry();

        ///valid criteria are validated
        SentinelRegistry.validateCriteria(Criteria.withLatestBlockId(randomUUID()));
        SentinelRegistry.validateCriteria(Criteria.builder()
            .latestBlockId(randomUUID())
            .artifactIdAndState(new ArtifactIdAndState(randomUUID(), 5))
            .build());
        SentinelRegistry.validateCriteria(Criteria.builder()
            .latestBlockId(randomUUID())
            .artifactId(randomUUID())
            .build());
        SentinelRegistry.validateCriteria(Criteria.builder()
            .latestBlockId(randomUUID())
            .artifactTypeId(randomUUID())
            .build());
        SentinelRegistry.validateCriteria(Criteria.builder()
            .latestBlockId(randomUUID())
            .transactionType(randomUUID())
            .build());

        // no error is thrown

        //null is passed
        try {
            SentinelRegistry.validateCriteria(null);
        } catch (InvalidCriteriaException ex) {
            assertThat(ex).isInstanceOf(InvalidCriteriaException.class);
        }

        try {
            SentinelRegistry.validateCriteria(Criteria.withLatestBlockId(null));
        } catch (NullPointerException ex) {
            // TODO: BP-235
            assertThat(ex).isInstanceOf(NullPointerException.class);
        }

        try {
            Criteria criteria = Criteria.builder()
                .latestBlockId(randomUUID())
                .artifactId(randomUUID())
                .artifactTypeId(randomUUID())
                .transactionType(randomUUID())
                .build();
            SentinelRegistry.validateCriteria(criteria);
        } catch (InvalidCriteriaException ex) {
            assertThat(ex).isInstanceOf(InvalidCriteriaException.class);
        }
    }
}
