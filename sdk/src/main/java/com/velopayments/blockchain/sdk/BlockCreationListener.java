package com.velopayments.blockchain.sdk;

import java.util.Optional;
import java.util.UUID;

public interface BlockCreationListener {

    Optional<UUID> getLatestNotifiedBlockId();


    void blockCreated(UUID latestBlockId);

    //TODO some sort of 'isAlive' to detect streams that have gone dead and need to be cleaned up
}
