package com.velopayments.blockchain.sdk.remoting;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This size-limited map forgets the oldest entries when it gets too big
 */
public class LimitedLinkedHashMap<K,V> extends LinkedHashMap<K,V> {
    private final int limit;

    public LimitedLinkedHashMap(int limit) {
        this.limit = limit;
    }

    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > limit;
    }

}
