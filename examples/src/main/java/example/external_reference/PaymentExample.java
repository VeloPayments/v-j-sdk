package example.external_reference;

import com.velopayments.blockchain.cert.*;
import com.velopayments.blockchain.client.TransactionStatus;
import com.velopayments.blockchain.sdk.BlockchainOperations;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.RemoteBlockchain;
import com.velopayments.blockchain.sdk.TransactionReader;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import com.velopayments.blockchain.sdk.metadata.CoreMetadata;
import com.velopayments.blockchain.sdk.vault.ExternalReference;
import com.velopayments.blockchain.sdk.vault.MutableFields;
import com.velopayments.blockchain.sdk.vault.VaultUtils;
import example.ExamplesConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.velopayments.blockchain.sdk.metadata.MetadataHelper.initMetadata;
import static example.external_reference.PaymentMetadata.*;
import static java.util.UUID.randomUUID;

/**
 * A simplistic Payment artifact transaction, demonstrating external references for:
 * <ul>
 * <li> storing an image of a receipt document</li>
 * <li> storing personally identifying information as mutable fields</li>
 * </ul>
 */
public class PaymentExample {

    public static void main(String[] args) throws Exception {
        ExamplesConfig config = ExamplesConfig.getInstance();

        EntityKeys connectionKeys = EntityTool.fromJson(config.getServiceEntityKeysConfigFile());
        RemoteBlockchain blockchain = RemoteBlockchain.builder()
            .entityKeys(connectionKeys)                   // (1)
            .agentHost(config.getHost())                  // (2)
            .agentPort(config.getAgentPort())             // (3)
            .vaultPort(config.getVaultPort())
            .maxAgentConnections(1)
            .build();

        final EntityKeys aliceKeys = EntityTool.fromJson(ExamplesConfig.getInstance().getEntityKeysConfigFile("alice", true));

        try (blockchain) {
            blockchain.start();
            //make sure that Velochain Explorer can visualise the data correctly, by storing the metadata
            initMetadata(blockchain, aliceKeys, PaymentMetadata.create());

            UUID paymentArtifactId = randomUUID();
            UUID transactionId = storePaymentTransaction(paymentArtifactId, blockchain, aliceKeys);

            readPaymentTransaction(transactionId, blockchain);

            if (Arrays.binarySearch(args, "no-delete") < 0) {
                //now do a GDPR 'right to be forgotten' and 'forget' all the mutable fields and the image
                blockchain.deleteExternalReferencesByArtifactId(paymentArtifactId);

                System.out.println();
                System.out.println("-- After deleting external references --");
                System.out.println();

                // the payment over again without external references
                readPaymentTransaction(transactionId, blockchain);
            }
        }
    }

    /**
     * Stores an example PAYMENT_CREATED transaction that includes external references to the receipt and personal
     * information.
     */
    static UUID storePaymentTransaction(UUID paymentArtifactId, BlockchainOperations blockchain, EntityKeys keys) throws Exception {
        // create a payment artifact to own the resource
        UUID paymentCreatedTxId = randomUUID();
        Certificate paymentCreatedTx = BlockchainUtils.transactionCertificateBuilder()
            .artifactType(PAYMENT_TYPE_ID)
            .artifactId(paymentArtifactId)
            .transactionType(CREATED.getId())
            .transactionId(paymentCreatedTxId)
            .previousTransactionId(BlockchainUtils.INITIAL_TRANSACTION_UUID)
            .withFields()
            .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey());
        wait(blockchain.submit(paymentCreatedTx));

        // load the receipt image and calculate a digital signature of that image
        String fileName = "/Receipt-Template.png";
        byte[] imageBytes = getFileContent(fileName);
        byte[] imageSignature = ExternalReference.createMessageDigest().digest(imageBytes);

        // create a file external reference for image of the receipt
        Certificate imageExternalReferenceCert = VaultUtils.externalReferenceBuilder()
            .referenceId(randomUUID())
            .artifactId(paymentArtifactId)
            .anchorField(PAYMENT_DOCUMENT.getId())
            .contentType("image/png")
            .contentLength((long) imageBytes.length)
            .signature(imageSignature)
            .withFields()
            .addString(CoreMetadata.EXTERNAL_REF_ORIG_FILE_NAME.getId(), fileName)
            .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey());

        // create certificate fragment holding personally identifying information and calculate its digital signature
        Certificate personalInformation = CertificateBuilder.createCertificateFragmentBuilder()
            .addString(CUSTOMER_EMAIL.getId(), "bob.smith@example.com")
            .addString(CUSTOMER_NAME.getId(), "Bob Smith")
            .emit();
        byte[] personalInformationBytes = personalInformation.toByteArray();
        byte[] personalInformationSignature = ExternalReference.createMessageDigest().digest(personalInformationBytes);

        // create an mutable fields external reference for the personal information
        Certificate mutableFieldsExternalRefCert = MutableFields.externalReferenceBuilder()
            .referenceId(UUID.randomUUID())
            .artifactId(paymentArtifactId)
            .schemaFrom(personalInformation)
            .contentLength((long) personalInformation.toByteArray().length)
            .signature(personalInformationSignature)
            .withFields()
            .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey());

        // store the image and the  mutable fields in Velochain, strongly anchored by the external reference certificates
        CompletableFuture<Certificate> futureResult1 =
            blockchain.addExternalReference(imageExternalReferenceCert, new ByteArrayInputStream(imageBytes));
        CompletableFuture<Certificate> futureResult2 =
            blockchain.addExternalReference(mutableFieldsExternalRefCert, new ByteArrayInputStream(personalInformationBytes));

        // the results are the same external reference certificates, now wrapped in vault-signed certificate
        Certificate signedImageExternalReferenceCert = wait(futureResult1);
        Certificate signedMutableFieldsExternalRefCert = wait(futureResult2);

        //store the payment, with both the image ref and the mutable fields ref
        UUID paymentUpdatedTxId = randomUUID();
        Certificate paymentUpdated = BlockchainUtils.transactionCertificateBuilder()
            .artifactType(PAYMENT_TYPE_ID)
            .artifactId(paymentArtifactId)
            .transactionType(UPDATED.getId())
            .transactionId(paymentUpdatedTxId)
            .previousTransactionId(paymentCreatedTxId)
            .previousState(CoreMetadata.CREATED_STATE.getValue())
            .newState(STATE_INSTRUCTED.getValue())
            .withFields()
            .addByteArray(VaultUtils.EXTERNAL_REF_SIGNED, signedImageExternalReferenceCert.toByteArray())       //EXTERNAL_REF_SIGNED is a multi-value field.
            .addByteArray(VaultUtils.EXTERNAL_REF_SIGNED, signedMutableFieldsExternalRefCert.toByteArray())     //Index 0 is the image, index 1 is the mutableFields
            .addString(ITEM_DESCRIPTION.getId(), "Milk chocolate")                //we also store some immutable fields
            .addString(PAYMENT_AMOUNT.getId(), "USD 123.45")                      //to show the difference between mutable and immutable data handling
            .sign(keys.getEntityId(), keys.getSigningKeyPair().getPrivateKey());

        // sumbit the artifact transaction to the blockchain
        TransactionStatus result = wait(blockchain.submit(paymentUpdated));
        if (result != TransactionStatus.SUCCEEDED) {
            throw new RuntimeException("Transaction did not succeed: " + result);
        }
        return paymentUpdatedTxId;
    }

    /**
     * Read back transaction and both of it's external references (image and mutable fields) from the blockchain
     */
    private static void readPaymentTransaction(UUID paymentCreatedTxId, BlockchainOperations blockchain) throws Exception {
        // load the transaction from blockchain
        TransactionReader transactionReader = blockchain.findTransactionById(paymentCreatedTxId)
            .orElseThrow(() -> new RuntimeException("Transaction not found!"));

        // externally held items need to be retrieved from the vault
        List<Certificate> externalReferences = transactionReader.getExternalReferences();
        Certificate imageExternalReferenceCert   = externalReferences.get(0);
        Certificate mutableFieldsExternalRefCert = externalReferences.get(1);

        // Retrieve the mutable fields
        ExternalReference mutableFieldsExternalReference = blockchain.resolveExternalReference(mutableFieldsExternalRefCert);
        printPayment(transactionReader, mutableFieldsExternalReference);

        // Retrieve the payment receipt image
        ExternalReference imageExternalReference = blockchain.resolveExternalReference(imageExternalReferenceCert);
        printPaymentReceipt(imageExternalReference);
    }

    private static void printPayment(TransactionReader transactionReader, ExternalReference externalReference) throws IOException {
        CertificateReader piiReader = null;
        if (externalReference.isPresent()) {
            byte[] externalData = externalReference.asByteArray();
            Certificate personalInformation = Certificate.fromByteArray(externalData);
            piiReader = new CertificateReader(new CertificateParser(personalInformation));
        }

        System.out.printf("Payment (Mutable fields present: %s)%n", externalReference.isPresent());
        System.out.println("======================================");
        String format = "%-26s : %s%n";
        System.out.printf(format, ITEM_DESCRIPTION.getName(), transactionReader.getFirst(ITEM_DESCRIPTION.getId()).asString());
        System.out.printf(format, PAYMENT_AMOUNT.getName(), transactionReader.getFirst(PAYMENT_AMOUNT.getId()).asString());
        System.out.printf(format, CUSTOMER_EMAIL.getName(), piiReader == null ? null : piiReader.getFirst(CUSTOMER_EMAIL.getId()).asString());
        System.out.printf(format, CUSTOMER_NAME.getName(), piiReader == null ? null : piiReader.getFirst(CUSTOMER_NAME.getId()).asString());
    }

    private static void printPaymentReceipt(ExternalReference externalReference) throws IOException {
        Integer contentLength = null;
        String signatureEnc = null;
        if (externalReference.isPresent()) {
            // we can verify the externally referenced data loaded from the vault matches the signature from the blockchain
            byte[] externalData = externalReference.asByteArray();
            byte[] signature = ExternalReference.createMessageDigest().digest(externalData);
            contentLength = externalData.length;
            signatureEnc = toBase64(signature);
        }
        System.out.println();
        System.out.println("Payment Receipt");
        System.out.println("===============");
        System.out.println("Present in vault           : " + externalReference.isPresent());
        System.out.println("Original filename          : " + externalReference.getOriginalFileName().orElse("(none)"));
        System.out.println("Media type                 : " + externalReference.getContentType());
        System.out.println("Recorded content length    : " + externalReference.getContentLength());
        System.out.println("Recorded signature         : " + externalReference.getSignatureEncoded());
        System.out.println("Found content length       : " + contentLength);
        System.out.println("Found image signature      : " + signatureEnc);
    }

    private static String toBase64(byte[] imageSignature) {
        return Base64.getEncoder().encodeToString(imageSignature);
    }

    private static byte[] getFileContent(String fileName) throws Exception {
        return PaymentExample.class.getResourceAsStream(fileName).readAllBytes();
    }

    private static <T> T wait(CompletableFuture<T> completableFuture) throws Exception {
        Thread.sleep(2 * 1000);
        return completableFuture.get(15, TimeUnit.SECONDS);
    }
}
