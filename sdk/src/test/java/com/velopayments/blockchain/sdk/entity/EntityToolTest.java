package com.velopayments.blockchain.sdk.entity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityToolTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generate() {
        EntityKeys generated = EntityTool.generate("Sam Sandwich");
        assertNonNullFields(generated);
        assertThat(generated.getSecretKey()).isNotNull();
    }

    @Test
    public void loadFromJsonFile() throws Exception {
        EntityKeys entityKeys = EntityTool.generate("Bubba Snitman");

        String encPrivKey = base64(entityKeys.getEncryptionKeyPair().getPrivateKey().getRawBytes());
        String encPubKey = base64(entityKeys.getEncryptionKeyPair().getPublicKey().getRawBytes());
        String signPrivKey = base64(entityKeys.getSigningKeyPair().getPrivateKey().getRawBytes());
        String signPubKey = base64(entityKeys.getSigningKeyPair().getPublicKey().getRawBytes());
        String skey = base64(entityKeys.getSecretKey().getRawBytes());
        String keyConfigJson =
            "{\n" +
                "    \"entityId\" : \"" + entityKeys.getEntityId() + "\",\n" +
                "    \"entityName\" : \"" + entityKeys.getEntityName() + "\",\n" +
                "    \"encryptionKey\" : {\n" +
                "        \"privateKeyBase64\" : \"" + encPrivKey + "\",\n" +
                "        \"publicKeyBase64\" : \"" + encPubKey + "\"\n" +
                "    },\n" +
                "    \"signingKey\" : {\n" +
                "        \"privateKeyBase64\" : \"" + signPrivKey + "\",\n" +
                "        \"publicKeyBase64\" : \"" + signPubKey + "\"\n" +
                "    },\n" +
                "    \"secretKey\" : \"" + skey + "\"\n" +
                "}\n";

        Path path = Files.write(
            temporaryFolder.newFile().toPath(),
            keyConfigJson.getBytes(StandardCharsets.UTF_8));

        // load the key from json
        EntityKeys fromJson = EntityTool.fromJson(path);

        assertNonNullFields(fromJson);

        assertThat(fromJson.getEntityId()).isEqualTo(entityKeys.getEntityId());

        assertThat(fromJson.getEntityName()).isEqualTo(entityKeys.getEntityName());

        assertThat(fromJson.getEncryptionKeyPair().getPrivateKey().getRawBytes())
            .isEqualTo(entityKeys.getEncryptionKeyPair().getPrivateKey().getRawBytes());

        assertThat(fromJson.getEncryptionKeyPair().getPublicKey().getRawBytes())
            .isEqualTo(entityKeys.getEncryptionKeyPair().getPublicKey().getRawBytes());

        assertThat(fromJson.getSigningKeyPair().getPrivateKey().getRawBytes())
            .isEqualTo(entityKeys.getSigningKeyPair().getPrivateKey().getRawBytes());

        assertThat(fromJson.getSigningKeyPair().getPublicKey().getRawBytes())
            .isEqualTo(entityKeys.getSigningKeyPair().getPublicKey().getRawBytes());

        assertThat(fromJson.getSecretKey().getRawBytes())
            .isEqualTo(entityKeys.getSecretKey().getRawBytes());
    }

    @Test
    public void loadFromOlderJsonFileWithoutSecretKey() throws Exception {
        EntityKeys entityKeys = EntityTool.generate("Alice");

        String encPrivKey = base64(entityKeys.getEncryptionKeyPair().getPrivateKey().getRawBytes());
        String encPubKey = base64(entityKeys.getEncryptionKeyPair().getPublicKey().getRawBytes());
        String signPrivKey = base64(entityKeys.getSigningKeyPair().getPrivateKey().getRawBytes());
        String signPubKey = base64(entityKeys.getSigningKeyPair().getPublicKey().getRawBytes());
        String keyConfigJson =
            "{\n" +
                "    \"entityId\" : \"" + entityKeys.getEntityId() + "\",\n" +
                "    \"entityName\" : \"" + entityKeys.getEntityName() + "\",\n" +
                "    \"encryptionKey\" : {\n" +
                "        \"privateKeyBase64\" : \"" + encPrivKey + "\",\n" +
                "        \"publicKeyBase64\" : \"" + encPubKey + "\"\n" +
                "    },\n" +
                "    \"signingKey\" : {\n" +
                "        \"privateKeyBase64\" : \"" + signPrivKey + "\",\n" +
                "        \"publicKeyBase64\" : \"" + signPubKey + "\"\n" +
                "    }\n" +
                "}\n";

        Path path = Files.write(
            temporaryFolder.newFile().toPath(),
            keyConfigJson.getBytes(StandardCharsets.UTF_8));

        // load the key from json
        EntityKeys fromJson = EntityTool.fromJson(path);

        assertNonNullFields(fromJson);

        assertThat(fromJson.getEntityId()).isEqualTo(entityKeys.getEntityId());

        assertThat(fromJson.getEntityName()).isEqualTo(entityKeys.getEntityName());

        assertThat(fromJson.getEncryptionKeyPair().getPrivateKey().getRawBytes())
            .isEqualTo(entityKeys.getEncryptionKeyPair().getPrivateKey().getRawBytes());

        assertThat(fromJson.getEncryptionKeyPair().getPublicKey().getRawBytes())
            .isEqualTo(entityKeys.getEncryptionKeyPair().getPublicKey().getRawBytes());

        assertThat(fromJson.getSigningKeyPair().getPrivateKey().getRawBytes())
            .isEqualTo(entityKeys.getSigningKeyPair().getPrivateKey().getRawBytes());

        assertThat(fromJson.getSigningKeyPair().getPublicKey().getRawBytes())
            .isEqualTo(entityKeys.getSigningKeyPair().getPublicKey().getRawBytes());

        assertThat(fromJson.getSecretKey()).isNull();
    }

    @Test
    public void mainMethodGenerateJson() throws Exception {
        PrintStream originalOut = System.out;
        try {
            // capture std out
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            // generate a key with name foo
            EntityTool.main(new String[]{ "foo" });

            // read json from output capture
            InputStream json = new ByteArrayInputStream(outContent.toByteArray());
            assertThat(json).isNotNull();

            // parse captured json
            EntityKeys keyConfig = EntityTool.fromJson(json);
            assertNonNullFields(keyConfig);
            assertThat(keyConfig.getSecretKey()).isNotNull();

        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void generateJsonContent() throws Exception {
        EntityKeys generated = EntityTool.generate("Sam Sandwich");
        assertNonNullFields(generated);
        assertThat(generated.getSecretKey()).isNotNull();

        // output as json
        String json = EntityTool.toJson(generated);
        assertThat(json).isNotNull();

        EntityKeys loaded = EntityTool.fromJson(json);
        assertNonNullFields(loaded);
        assertThat(loaded.getEntityId()).isEqualTo(generated.getEntityId());
        assertThat(loaded.getEntityName()).isEqualTo(generated.getEntityName());
        assertThat(loaded.getSecretKey().getRawBytes()).isEqualTo(generated.getSecretKey().getRawBytes());
        assertThat(loaded.getEncryptionKeyPair().getPublicKey().getRawBytes())
            .isEqualTo(generated.getEncryptionKeyPair().getPublicKey().getRawBytes());
        assertThat(loaded.getEncryptionKeyPair().getPrivateKey().getRawBytes())
            .isEqualTo(generated.getEncryptionKeyPair().getPrivateKey().getRawBytes());
        assertThat(loaded.getSigningKeyPair().getPublicKey().getRawBytes())
            .isEqualTo(generated.getSigningKeyPair().getPublicKey().getRawBytes());
        assertThat(loaded.getSigningKeyPair().getPrivateKey().getRawBytes())
            .isEqualTo(generated.getSigningKeyPair().getPrivateKey().getRawBytes());
    }

    private String base64(byte[] rawBytes) {
        return Base64.getEncoder().encodeToString(rawBytes);
    }

    private void assertNonNullFields(EntityKeys keyConfig) {
        assertThat(keyConfig).isNotNull();
        assertThat(keyConfig.getEntityId()).isNotNull();
        assertThat(keyConfig.getEncryptionKeyPair()).isNotNull();
        assertThat(keyConfig.getSigningKeyPair()).isNotNull();
    }
}
