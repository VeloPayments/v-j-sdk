package com.velopayments.blockchain.sdk.entity;

import com.velopayments.blockchain.crypt.EncryptionKeyPair;
import com.velopayments.blockchain.crypt.Key;
import com.velopayments.blockchain.crypt.SigningKeyPair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.velopayments.blockchain.sdk.entity.EntityKeyConfigContentType.UnprotectedOnly;

/**
 * A Utility for generating, loading and saving of EntityKeys.
 */
public final class EntityTool {

    private EntityTool() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Generates EntityKeys and outputs in JSON format, printed to system out.  Intended to be used as:
     * <pre>{@code
     *   java -cp velochain-all.jar com.velopayments.blockchain.sdk.EntityTool "John Doe" > ./john_doe.keyconfig
     * }</pre>
     *
     * @param args Requires a single argument for the name of the entity.
     */
    public static void main(String[] args)  {
        if (args.length < 1) {
            throw new IllegalStateException("Name is required as an argument");
        }
        System.out.println(generateAsJson(args[0]));
    }

    public static String toJson(EntityKeys entityKeys) {
        return EntityKeysSerializer.toJson((EntityKeyConfig) entityKeys);
    }

    public static String generateAsJson(String name) {
        return toJson(generate(name));
    }

    public static EntityKeys generate(String entityName) {
        return EntityKeyConfig.builder()
            .entityId(UUID.randomUUID())
            .entityName(entityName)
            .contentType(UnprotectedOnly)
            .signingKeyPair(SigningKeyPair.generate())
            .encryptionKeyPair(EncryptionKeyPair.generate())
            .secretKey(Key.createRandom())
            .build();
    }

    public static EntityKeys fromJson(InputStream is) {
        return EntityKeysSerializer.fromJson(is);
    }

    public static EntityKeys fromJson(String json) {
        return EntityKeysSerializer.fromJson(json);
    }

    public static EntityKeys fromJson(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Entity keys not found: " + path.toAbsolutePath());
        }
        try {
            return EntityKeysSerializer.fromJson(Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open entity keys from " + path, e);
        }
    }
}
