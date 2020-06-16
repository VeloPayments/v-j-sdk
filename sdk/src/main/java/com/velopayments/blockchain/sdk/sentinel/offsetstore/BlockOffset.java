package com.velopayments.blockchain.sdk.sentinel.offsetstore;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class BlockOffset {

    private UUID blockId;

    private Long blockHeight;

}
