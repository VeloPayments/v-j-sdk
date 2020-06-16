package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.crypt.SigningKeyPair;

import java.util.UUID;

public interface Signer {
    /**
     * The id of the entity.
     * @return a non-null UUID
     */
    UUID getEntityId();

    /**
     * The asymmetric key pair to use for producing digital signatures.
     * @return a key pair or null
     */
    SigningKeyPair getSigningKeyPair();
}
