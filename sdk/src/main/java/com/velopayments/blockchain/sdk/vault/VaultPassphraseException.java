package com.velopayments.blockchain.sdk.vault;

public class VaultPassphraseException extends VaultException {

    public VaultPassphraseException() {
        super("The vault passphrase was incorrect");
    }
}
