package com.velopayments.blockchain.sdk.remoting;

import java.util.Collection;
import java.util.LinkedList;

/**
 * This size-limited queue forgets the oldest entries when it gets too big
 */
public class LimitedQueue<E> extends LinkedList<E> {
    private final int limit;

    public LimitedQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        super.add(o);
        drain();
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        super.addAll(c);
        drain();
        return true;
    }

    private void drain() {
        while (size() > limit) { remove(); }
    }

}
