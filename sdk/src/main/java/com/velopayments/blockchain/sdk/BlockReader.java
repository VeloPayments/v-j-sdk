package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.cert.CertificateParser;
import com.velopayments.blockchain.cert.CertificateReader;
import com.velopayments.blockchain.cert.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.velopayments.blockchain.cert.Field.*;

public class BlockReader extends CertificateReaderSupport {

    public BlockReader(Certificate cert) {
        super(cert);
    }

    public BlockReader(CertificateReader certificateReader) {
        super(certificateReader);
    }

    public Long getBlockHeight() {
        return getLong(BLOCK_HEIGHT);
    }

    public UUID getBlockId() {
        return getUUID(BLOCK_UUID);
    }

    public UUID getPreviousBlockId() {
        return getUUID(PREVIOUS_BLOCK_UUID);
    }

    //TODO: seems like this would benefit from being lazy
    public List<TransactionReader> getTransactions() {
        List<TransactionReader> transactions = new ArrayList<>();
        for (int i = 0; i < this.certificateReader.count(Field.WRAPPED_TRANSACTION_TUPLE); ++i) {
            byte[] certdata = this.certificateReader.get(WRAPPED_TRANSACTION_TUPLE, i).asByteArray();
            CertificateReader certReader = new CertificateReader(new CertificateParser(Certificate.fromByteArray(certdata)));
            transactions.add(new TransactionReader(certReader));
        }
        return transactions;
    }
}
