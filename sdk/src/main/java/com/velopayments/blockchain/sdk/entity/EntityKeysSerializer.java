package com.velopayments.blockchain.sdk.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.velopayments.blockchain.crypt.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class EntityKeysSerializer {

    private static final String privateKeyFieldName = "privateKeyBase64";
    private static final String publicKeyFieldName = "publicKeyBase64";

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new KeyPairModule())
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);


    static EntityKeyConfig fromJson(InputStream inputStream) {
        try {
            return objectMapper.readValue(requireNonNull(inputStream), EntityKeyConfig.class);
        }
        catch (IOException e) {
            throw new RuntimeException("Error unmarshalling EntityKey config from JSON", e);
        }
    }

    static EntityKeyConfig fromJson(String json) {
        return fromJson(new ByteArrayInputStream(requireNonNull(json).getBytes()));
    }

    static String toJson(EntityKeyConfig entityKeys) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Objects.requireNonNull(entityKeys));
        }
        catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write as json", e);
        }
    }


    private static final class KeyPairModule extends SimpleModule {
        KeyPairModule() {
            addDeserializer(EncryptionKeyPair.class, new KeyPairDeserializer<>() {
                @Override
                protected EncryptionKeyPair createPair(byte[] publicKey, byte[] privateKey) {
                    return new EncryptionKeyPair(
                        new EncryptionPublicKey(publicKey),
                        new EncryptionPrivateKey(privateKey)
                    );
                }
            });

            addDeserializer(SigningKeyPair.class, new KeyPairDeserializer<>() {
                @Override
                protected SigningKeyPair createPair(byte[] publicKey, byte[] privateKey) {
                    return new SigningKeyPair(
                        new SigningPublicKey(publicKey),
                        new SigningPrivateKey(privateKey)
                    );
                }
            });

            addDeserializer(Key.class, new KeyDeserializer());

            addSerializer(EncryptionKeyPair.class, new KeyPairSerializer<>() {
                @Override
                protected byte[] getPrivateKey(EncryptionKeyPair keypair) {
                    return keypair == null ? null : keypair.getPrivateKey().getRawBytes();
                }

                @Override
                protected byte[] getPublicKey(EncryptionKeyPair keypair) {
                    return keypair == null ? null : keypair.getPublicKey().getRawBytes();
                }
            });

            addSerializer(SigningKeyPair.class, new KeyPairSerializer<>() {
                @Override
                protected byte[] getPrivateKey(SigningKeyPair keypair) {
                    return keypair == null ? null : keypair.getPrivateKey().getRawBytes();
                }

                @Override
                protected byte[] getPublicKey(SigningKeyPair keypair) {
                    return keypair == null ? null : keypair.getPublicKey().getRawBytes();
                }
            });

            addSerializer(Key.class, new KeySerializer());
        }
    }

    abstract static class KeyPairDeserializer<T> extends StdDeserializer<T> {
        public KeyPairDeserializer() {
            this(null);
        }

        public KeyPairDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public T deserialize(JsonParser parser, DeserializationContext deserializer) throws IOException {
            ObjectCodec codec = parser.getCodec();
            JsonNode node = codec.readTree(parser);
            JsonNode privateKeyNode = node.get(privateKeyFieldName);
            JsonNode publicKeyNode = node.get(publicKeyFieldName);
            return createPair(publicKeyNode.binaryValue(), privateKeyNode.binaryValue());
        }

        abstract protected T createPair(byte[] publicKey, byte[] privateKey);
    }

    abstract static class KeyPairSerializer<T> extends StdSerializer<T> {
        public KeyPairSerializer() {
            this(null);
        }

        public KeyPairSerializer(Class<T> t) {
            super(t);
        }

        @Override
        public void serialize(T keypair, JsonGenerator jsonGenerator, SerializerProvider serializer) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeBinaryField(privateKeyFieldName, getPrivateKey(keypair));
            jsonGenerator.writeBinaryField(publicKeyFieldName, getPublicKey(keypair));
            jsonGenerator.writeEndObject();
        }

        protected abstract byte[] getPrivateKey(T keypair);
        protected abstract byte[] getPublicKey(T keypair);
    }

    static class KeySerializer extends StdSerializer<Key> {
        public KeySerializer() {
            super(Key.class);
        }

        @Override
        public void serialize(Key key, JsonGenerator jsonGenerator, SerializerProvider serializer) throws IOException {
            jsonGenerator.writeBinary(key.getRawBytes());
        }
    }

    static class KeyDeserializer extends StdDeserializer<Key> {
        public KeyDeserializer() {
            super(Key.class);
        }

        @Override
        public Key deserialize(JsonParser parser, DeserializationContext deserializer) throws IOException {
            return new Key(parser.getBinaryValue());
        }
    }
}
