package com.velopayments.blockchain.sdk.guard;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GuardRegistry {

    private final Set<PreSubmitGuard> guards = new HashSet<>();

    public GuardRegistry() {
    }

    public GuardRegistry(Collection<PreSubmitGuard> guards) {
        guards.forEach(this::register);
    }

    public void register(PreSubmitGuard guard) {
        guards.add(guard);
    }

    public boolean unregister(PreSubmitGuard guard) {
        return guards.remove(guard);
    }

    public Set<PreSubmitGuard> getPreSumbitGuards() {
        return Collections.unmodifiableSet(guards);
    }
}
