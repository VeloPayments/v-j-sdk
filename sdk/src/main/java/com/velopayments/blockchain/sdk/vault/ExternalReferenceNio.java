package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.Certificate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.READ;

/**
 * A ExternalReference backed by an NIO Path.
 */
public class ExternalReferenceNio extends ExternalReferenceSupport {

    private final Path path;

    public ExternalReferenceNio(Path path, Certificate certificate) {
        super(certificate);
        this.path = Objects.requireNonNull(path);
    }

    /**
     * @see ExternalReference#isPresent()
     */
    @Override
    public boolean isPresent() {
        return Files.exists(path, NOFOLLOW_LINKS);
    }

    /**
     * @see ExternalReference#read()
     */
    @Override
    public InputStream read() throws IOException {
        var signature = getByteArray(VaultUtils.EXTERNAL_REF_SIGNATURE);
        return new ValidatingInputStream(signature, Files.newInputStream(path, READ));
    }
}
