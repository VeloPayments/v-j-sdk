package com.velopayments.blockchain.sdk.sentinel.criteria;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Criteria {

    /**
     * What was the latest block id when you last viewed the data
     */
    @NonNull
    private UUID latestBlockId;

    //Provide exactly zero or one of the following
    //If you only provide latestBlockId then you'll be notified as soon as there is a new block

    /**
     * Listen for state changes on an artifact
     */
    private ArtifactIdAndState artifactIdAndState;

    /**
     * Listen for any new transactions on an artifact
     */
    private UUID artifactId;

    /**
     * Listen for new transactions on an any artifact of this type
     */
    private UUID artifactTypeId;

    /**
     * Listen for transactions of this type
     */
    private UUID transactionType;

    public static Criteria withLatestBlockId(UUID blockId) {
        return Criteria.builder().latestBlockId(blockId).build();
    }

    @JsonIgnore
    public Criteria validate() throws InvalidCriteriaException {
        if (getLatestBlockId() == null) {
            throw new InvalidCriteriaException("latestBlockId is mandatory");
        }

        int numberOfAdditionalArgs = 0;
        if (getArtifactIdAndState() != null) {
            numberOfAdditionalArgs ++;
        }
        if (getArtifactId() != null) {
            numberOfAdditionalArgs ++;
        }
        if (getArtifactTypeId() != null) {
            numberOfAdditionalArgs ++;
        }
        if (getTransactionType() != null) {
            numberOfAdditionalArgs ++;
        }

        if (numberOfAdditionalArgs > 1) {
            throw new InvalidCriteriaException("You may only pass latestBlockId plus a maximum of one additional argument");
        }

        return this;
    }

    /**
     * Copies the criteria but uses the blockId from the argument
     * @param blockId the id of the block
     * @return an optional with the criteria or empty if blockId is null
     */
    public Optional<Criteria> withBlockId(UUID blockId) {
        return Optional.ofNullable(blockId).map(b ->
            toBuilder().latestBlockId(b).build()   //return the same criteria but with an updated latestBlockId
        );
    }
}
