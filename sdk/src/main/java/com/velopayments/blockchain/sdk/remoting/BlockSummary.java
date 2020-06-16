package com.velopayments.blockchain.sdk.remoting;

import com.velopayments.blockchain.sdk.BlockReader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockSummary {

    private UUID blockId;
    private long blockHeight;   //-1 indicates an empty block that should be ignored

    @Builder.Default
    private List<TransactionSummary> transactions = new ArrayList<>();

    public static BlockSummary fromBlockCertificateReader(BlockReader blockReader) {
        List<TransactionSummary> transactions = blockReader.getTransactions().stream()
            .map(TransactionSummary::transformTransaction)
            .collect(toList());
        Long height = Objects.requireNonNull(blockReader.getBlockHeight(), "BLOCK_HEIGHT field is absent");
        return BlockSummary.builder()
            .blockId(blockReader.getBlockId())
            .blockHeight(height)
            .transactions(transactions)
            .build();
    }
}
