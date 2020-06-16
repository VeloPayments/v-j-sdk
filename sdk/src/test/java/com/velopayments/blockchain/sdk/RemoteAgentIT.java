package com.velopayments.blockchain.sdk;

import com.velopayments.blockchain.cert.*;
import com.velopayments.blockchain.client.RemoteAgentConfiguration;
import com.velopayments.blockchain.client.RemoteAgentConnection;
import com.velopayments.blockchain.crypt.EncryptionPrivateKey;
import com.velopayments.blockchain.crypt.SigningKeyPair;

import javax.net.SocketFactory;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * This is a temporary utility class while developing the remoting capabilities.
 * It will be replaced with an integration test.
 */
public class RemoteAgentIT {

    public static void main(String[] args) {
        System.out.println("testing agentd connection...");

        UUID agentId = UUID.fromString("cb6c02aa-605f-4f81-bb01-5bb6f5975746");
        RemoteAgentConfiguration config = new RemoteAgentConfiguration(
            "localhost",4931,agentId,null);

        UUID entityId = UUID.fromString("aca029b6-2602-4b20-a8a4-cd8a95985a9a");

        byte[] entityPrivateKeyBytes = {
            (byte)0x77, (byte)0x07, (byte)0x6d, (byte)0x0a, (byte)0x73, (byte)0x18, (byte)0xa5, (byte)0x7d,
            (byte)0x3c, (byte)0x16, (byte)0xc1, (byte)0x72, (byte)0x51, (byte)0xb2, (byte)0x66, (byte)0x45,
            (byte)0xdf, (byte)0x4c, (byte)0x2f, (byte)0x87, (byte)0xeb, (byte)0xc0, (byte)0x99, (byte)0x2a,
            (byte)0xb1, (byte)0x77, (byte)0xfb, (byte)0xa5, (byte)0x1d, (byte)0xb9, (byte)0x2c, (byte)0x2a };

        SigningKeyPair signPair = SigningKeyPair.generate();

        RemoteAgentConnection conn = new RemoteAgentConnection(
            config, SocketFactory.getDefault(),
            entityId, new EncryptionPrivateKey(entityPrivateKeyBytes));

        try {
            conn.connect();
            System.out.println("handshake success!");
            UUID firstId = conn.getLatestBlockId().join();
            System.out.println("Initial Block UUID: " + firstId);
            for (int i = 0; i < 200; ++i) {
                System.out.println("Submitting txn: " + i);
                Certificate txn = makeTransactionCertificate(signPair, entityId);
                conn.submit(txn);
            }
            System.out.println("submit success!");

            Optional<UUID> prevIdMaybe = Optional.of(conn.getLatestBlockId().join());

            while (prevIdMaybe.isPresent()) {
                UUID prevId = prevIdMaybe.get();
                System.out.println("New Block UUID: " + prevId);
                if (prevId.compareTo(firstId) != 0) {
                    Optional<Certificate> blockCert = conn.getBlockById(prevId).join();
                    if (blockCert.isPresent()) {
                        System.out.println("Read block " + prevId + " from agentd");
                        readBlockCert(blockCert.get());
                    } else {
                        System.out.println("Could not retrieve block " + prevId);
                    }
                }

                prevIdMaybe = conn.getPrevBlockId(prevId).join();
                if (!prevIdMaybe.isPresent()) {
                    System.out.println("No other block IDs found.");
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("execution complete.");
    }

    private static void readBlockCert(Certificate cert) {

        CertificateReader reader =
            new CertificateReader(
                new CertificateParser(cert));

        System.out.println(
            "Found " +
                Integer.valueOf(reader.count(Field.WRAPPED_TRANSACTION_TUPLE)) +
                " transactions in cert.");

        for (int i = 0; i < reader.count(Field.WRAPPED_TRANSACTION_TUPLE); ++i)
        {
            try {
                readTransaction(
                    reader.get(
                        Field.WRAPPED_TRANSACTION_TUPLE, i).asByteArray());
            } catch (MissingFieldException e) {
                System.out.println("Missing transaction " + Integer.valueOf(i));
            }
        }
    }

    private static void readTransaction(byte[] txn) {
        try {
            CertificateReader reader =
                new CertificateReader(
                    new CertificateParser(
                        Certificate.fromByteArray(txn)));

            UUID transactionID = reader.getFirst(Field.CERTIFICATE_ID).asUUID();

            System.out.println("    Parsed transaction " + transactionID);
        } catch (Throwable e) {
            System.out.println("Could not parse transaction.");
        }
    }

    private static UUID DUMMY_TXN_TYPE =
        UUID.fromString("bc91987a-d2bd-46d7-bccb-a8d94ff49906");

    private static Certificate makeTransactionCertificate(
        SigningKeyPair signPair, UUID entityId) {

        UUID DUMMY_CERTIFICATE_ID = UUID.randomUUID();
        UUID DUMMY_ARTIFACT_ID = UUID.randomUUID();
        UUID ZERO_UUID = new UUID(0, 0);

        CertificateBuilder builder =
            CertificateBuilder.createCertificateBuilder(DUMMY_TXN_TYPE);
        builder.addUUID(Field.PREVIOUS_CERTIFICATE_ID, ZERO_UUID);
        builder.addUUID(Field.CERTIFICATE_ID, DUMMY_CERTIFICATE_ID);
        builder.addUUID(Field.ARTIFACT_ID, DUMMY_ARTIFACT_ID);
        builder.addInt(Field.PREVIOUS_ARTIFACT_STATE, -1);
        builder.addInt(Field.NEW_ARTIFACT_STATE, 0x00000000);
        builder.addString(0x405, "An Example String");
        return builder.sign(entityId, signPair.getPrivateKey());
    }
}
