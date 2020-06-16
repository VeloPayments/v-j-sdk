package com.velopayments.blockchain.sdk.vault;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.util.Arrays;

/**
 *  Validates that the data read matches the expect hash.
 *  <p>
 *  Validates the digest once -1 has been read so we must not validate twice.
 */
public class ValidatingInputStream extends DigestInputStream {

    //TODO: an immutable byte buffer would be wise here
    private final byte[] expectedHash;

    private boolean validated = false;

    public ValidatingInputStream(byte[] expectedSignature, InputStream stream) {
        super(stream, ExternalReference.createMessageDigest());
        this.expectedHash = expectedSignature;
    }

    @Override
    public int read() throws IOException {
        int ch = super.read();
        if (ch == -1) {
            // end of stream
            validate();
        }
        return ch;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (result == -1) {
            // end of stream
            validate();
        }
        return result;
    }

    private void validate() throws IOException {
        if (!validated) {
            validated = true;
            if (!Arrays.equals(digest.digest(), expectedHash)) {
                throw new IOException("Unable to verify integrity of data. The expected hash did not match.");
            }
        }
    }
}
