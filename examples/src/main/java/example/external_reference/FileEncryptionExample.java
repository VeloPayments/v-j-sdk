package example.external_reference;

import com.velopayments.blockchain.document.EncryptedDocumentBuilder;
import com.velopayments.blockchain.document.EncryptedDocumentReader;
import com.velopayments.blockchain.sdk.entity.EntityKeys;
import com.velopayments.blockchain.sdk.entity.EntityTool;
import example.ExamplesConfig;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A simple demonstration of sharing an encrypted document for specific subscribers.
 * <ul>
 * <li> Encrypting a document to be shared</li>
 * <li> Decrypting the document using a shared secret</li>
 * </ul>
 */
public class FileEncryptionExample {

    // A sample plain text document to encrypt and store
    static final String DOCUMENT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt.";

    public static void main(String[] args) throws Exception {
        ExamplesConfig config = ExamplesConfig.getInstance();

        // the entities involved are represented with keys
        final EntityKeys bob = EntityTool.fromJson(config.getEntityKeysConfigFile("bob", true));
        final EntityKeys alice = EntityTool.fromJson(config.getEntityKeysConfigFile("alice", true));
        final EntityKeys sam = EntityTool.fromJson(config.getEntityKeysConfigFile("sam", true));

        System.out.println("Original document: " + DOCUMENT);

        // store the encrypted document
        Path encryptedFile = Files.createTempFile("encrypted-doc", null);

        // (1) Bob encrypts the document with encryption keys
        EncryptedDocumentBuilder builder = EncryptedDocumentBuilder.createDocumentBuilder(bob.getEncryptionKeyPair());
        try (InputStream input = new ByteArrayInputStream(DOCUMENT.getBytes(Charset.defaultCharset()));
             OutputStream output = new FileOutputStream(encryptedFile.toFile())) {
                builder.withSource(input)
                    .withDestination(output)
                    .encrypt();
        }
        // Alice and Same provide Bob with their public encryption keys
        // Bob creates shared secrets for each entity for use in decryption the document
        byte[] alicesSharedSecret = builder.createEncryptedSharedSecret(alice.getEncryptionKeyPair().getPublicKey());
        byte[] samsSharedSecret = builder.createEncryptedSharedSecret(sam.getEncryptionKeyPair().getPublicKey());

        // (2A) sometime later, Alice can decrypt the document using her shared secret
        try (InputStream input = new FileInputStream(encryptedFile.toFile());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new EncryptedDocumentReader(
                    alice.getEncryptionKeyPair().getPrivateKey(),          // the private key of the entity doing the decryption
                    bob.getEncryptionKeyPair().getPublicKey(),             // the Bob's public key
                    alicesSharedSecret)
                .withSource(input)
                .withDestination(output)
                .decrypt();
            System.out.println("Alice's decrypted document: " + output.toString(Charset.defaultCharset()));
        }

        // (2B) Sam can also decrypt the document
        try (InputStream input = new FileInputStream(encryptedFile.toFile());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new EncryptedDocumentReader(
                    sam.getEncryptionKeyPair().getPrivateKey(),          // the private key of the entity doing the decryption
                    bob.getEncryptionKeyPair().getPublicKey(),           // the Bob's public key
                    samsSharedSecret)
                .withSource(input)
                .withDestination(output)
                .decrypt();
            System.out.println("Sam's decrypted document: " +  output.toString(Charset.defaultCharset()));
        }
    }
}
