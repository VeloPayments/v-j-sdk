package com.velopayments.blockchain.sdk.entity;

import com.velopayments.blockchain.crypt.EncryptionKeyPair;
import com.velopayments.blockchain.crypt.Key;
import com.velopayments.blockchain.sdk.Signer;

/**
 * An interface for providing signing, encryption and secret keys for a specific entity.
 */
public interface EntityKeys extends Signer {

    /**
     * The name of the entity.
     * @return a String with the name or null
     */
    String getEntityName();

    /**
     * The asymmetric key pair to use for encryption.
     * @return a key pair or null
     */
    EncryptionKeyPair getEncryptionKeyPair();

    /**
     * A symmetric secret key.
     * @return the key or null
     */
    Key getSecretKey();
}
