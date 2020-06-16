package com.velopayments.blockchain.sdk.sentinel;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class QueryResult<T> {
    private UUID latestBlockId;     //the caller needs to know what the latest block id was when the query was executed, so it can understand the point in time of the results
    private T result;
}
