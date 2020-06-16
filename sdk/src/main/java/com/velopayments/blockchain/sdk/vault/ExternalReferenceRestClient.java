package com.velopayments.blockchain.sdk.vault;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.CertificateReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.Function;

/**
 * A ExternalReference backed by remote request.
 */
public class ExternalReferenceRestClient extends ExternalReferenceSupport {

    private final Function<CertificateReader,HttpResponse<InputStream>> responseSupplier;

    private HttpResponse<InputStream> response;

    public ExternalReferenceRestClient(Certificate certificate, Function<CertificateReader,HttpResponse<InputStream>> responseSupplier) {
        super(certificate);
        this.responseSupplier = Objects.requireNonNull(responseSupplier);
    }

    /**
     * Make a request. If we are not going to read the stream, hold onto the response instance to read later.
     */
    private synchronized HttpResponse<InputStream> doRequest(boolean takeContent) {
        HttpResponse<InputStream> ret;
        if (needsRequest()) {
            HttpResponse<InputStream> resp = responseSupplier.apply(getCertificateReader());
            if (resp.statusCode() == 200 || resp.statusCode() == 404) {
                this.response = resp;
            }
            ret = resp;
        } else {
            ret = this.response;
        }
        return ret;
    }

    private boolean needsRequest() {
        try {
            return this.response == null || this.response.body().available() == 0;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @see ExternalReference#isPresent()
     */
    @Override
    public boolean isPresent() {
        HttpResponse<InputStream> response = doRequest(false);
        switch (response.statusCode()) {
            case 404: return false;
            case 200: return true;
            default: throw new VaultException("Failed to get resource status for " + getExternalReferenceId() + ": " + response.statusCode());
        }
    }

    /**
     * @see ExternalReference#read()
     */
    @Override
    public InputStream read() throws IOException {
        HttpResponse<InputStream> response = doRequest(true);
        var signature = getByteArray(VaultUtils.EXTERNAL_REF_SIGNATURE);
        return new ValidatingInputStream(signature, response.body());
    }
}
