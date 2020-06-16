package com.velopayments.blockchain.sdk.sentinel.criteria;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArtifactIdAndState {

    @NonNull
    private UUID artifactId;

    @NonNull
    private Integer state;
}
