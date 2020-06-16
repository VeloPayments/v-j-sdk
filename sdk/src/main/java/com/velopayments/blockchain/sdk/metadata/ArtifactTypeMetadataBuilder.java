package com.velopayments.blockchain.sdk.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velopayments.blockchain.cert.CertificateBuilder;
import com.velopayments.blockchain.sdk.BlockchainIOException;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.TransactionReader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.velopayments.blockchain.sdk.BlockchainUtils.INITIAL_TRANSACTION_UUID;
import static com.velopayments.blockchain.sdk.metadata.CoreMetadata.CORE_METADATA_TYPE_ID;
import static com.velopayments.blockchain.sdk.metadata.CoreMetadata.DISPLAY_NAME;
import static java.util.UUID.randomUUID;

@Slf4j
public class ArtifactTypeMetadataBuilder {

    private final ArtifactTypeMetadata metadata;

    public ArtifactTypeMetadataBuilder(UUID artifactTypeId, String artifactTypeName) {
        this.metadata = new ArtifactTypeMetadata(
            assertNotNull(artifactTypeId, "artifactTypeId cannot be null"),
            validateUpperCaseUnderscores(artifactTypeName));
    }

    public ArtifactTypeMetadataBuilder(ArtifactTypeMetadata metadata) {
        this.metadata = metadata;
    }

    public ArtifactTypeMetadataBuilder addTransactionType(UUID transactionId, String name) {
        return addTransactionType(TransactionType.builder()
            .id(transactionId)
            .name(name)
            .build());
    }

    public ArtifactTypeMetadataBuilder addTransactionType(TransactionType transaction) {
        assertNotNull(transaction, "transaction cannot be null");
        assertNotNull(transaction.getId(), "transaction id cannot be null");
        assertNotNull(transaction.getName(), "transaction name cannot be null");

        validateUpperCaseUnderscores(transaction.getName());
        metadata.addTransaction(transaction);
        return this;
    }

    public ArtifactTypeMetadataBuilder addField(int id, String name, FieldType type) {
        return addField(FieldMetadata.builder()
            .id(id)
            .name(name)
            .type(type)
            .build());
    }

    public ArtifactTypeMetadataBuilder addField(FieldMetadata field) {
        assertNotNull(field, "field cannot be null");
        if (field.getId() == 0) {
            throw new IllegalMetadataException("field id cannot be zero");
        }
        assertNotNull(field.getName(), "field name cannot be null");
        assertNotNull(field.getType(), "field type cannot be null");
        validateUpperCaseUnderscores(field.getName());

        metadata.addField(field);
        return this;
    }


    public ArtifactTypeMetadataBuilder withSearchOptions(SearchOptions searchOptions) {
        metadata.setSearchOptions(assertNotNull(searchOptions, "searchOptions may not be null"));
        return this;
    }

    public ArtifactTypeMetadataBuilder addState(int value, String name) {
        return addState(ArtifactState.builder()
            .value(value)
            .name(name)
            .build()
        );
    }

    public ArtifactTypeMetadataBuilder addState(ArtifactState state) {
        assertNotNull(state.getName(), "state name cannot be null");
        assertNotNull(state, "state cannot be null");
        log.trace("State Name: {}, Value: {} ", state.getName(), state.getValue());
        if (state.getValue() == 0 && !state.getName().startsWith("_")) { // allow core meta
            throw new IllegalMetadataException("state value cannot be zero");
        }
        validateUpperCaseUnderscores(state.getName());

        metadata.addState(state);
        return this;
    }

    private String validateUpperCaseUnderscores(String upperCase) {
        if (
                upperCase.contains(" ")                         //check for spaces
                || !upperCase.toUpperCase().equals(upperCase)   //check for upper case
                || !upperCase.matches("^[_a-zA-Z0-9]+$")
            ) {
            String msg = String.format("[%s] must only contain upper case alphanumerics or underscores", upperCase);
            throw new IllegalMetadataException(msg);
        }
        return upperCase;
    }

    public static <T> T assertNotNull(T obj) {
        return assertNotNull(obj, "Object may not be null");
    }

    public static <T> T assertNotNull(T obj, String msg) {
        if (obj == null) {
            throw new IllegalMetadataException(msg);
        }
        return obj;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    CertificateBuilder build() {
        return build(INITIAL_TRANSACTION_UUID);    //ALL_ZEROS_UUID indicates that this is the first transaction in the lifetime of this artifact type
    }

    /**
     * Build a certificate with the {@link ArtifactTypeMetadata} embedded
     *
     * @param prevTransactionId - LocalBlockchain.ALL_ZEROS_UUID indicates that this is the first transaction in the lifetime of this artifact type
     *
     * @return CertificateBuilder with a certificate
     */
    CertificateBuilder build(UUID prevTransactionId) {
        UUID transactionId = randomUUID();
        TransactionType transactionType = INITIAL_TRANSACTION_UUID.equals(prevTransactionId) ? ArtifactTypeType.TYPE_CREATED : ArtifactTypeType.TYPE_UPDATED;

        //TODO compress json
        //byte[] json = compressString(toJson(metadata));
        String json = toJson(metadata);
        log.debug("[{}] Building {} artifact {} transaction cert {} -> prev {}\n{}", metadata.getArtifactTypeName(), metadata.getArtifactTypeId(),  transactionType.getName(), transactionId, prevTransactionId, json);
        return BlockchainUtils.transactionCertificateBuilder()
            .transactionId(transactionId)
            .previousTransactionId(prevTransactionId)
            .transactionType(transactionType.getId())
            .artifactId(metadata.getArtifactTypeId())
            .artifactType(ArtifactTypeType.ARTIFACT_TYPE_TYPE_TYPE_ID)
            .withFields()
            .addString(CoreMetadata.DISPLAY_NAME.getId(), metadata.getArtifactTypeName() + " type information")
            .addString(ArtifactTypeType.TYPE_METADATA_JSON.getId(), json); // TODO: why encode to json instead of cert
    }

    /**
     * This class saves storage space on the certificate and allows ArtifactTypeMetadata to do some stuff for caching, like fieldsByCamelCaseFieldName, without impacting storage space
     * And also the parent type reference
     */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    static class StrippedDownArtifactTypeMetadata {
        private UUID artifactTypeId;
        private String artifactTypeName;
        private UUID parentTypeId;
        private SearchOptions searchOptions;
        private SortedSet<TransactionType> transactionTypes;    //has to be sorted because we do a JSON string comparison to check whether core metadata is up to date
        private SortedSet<FieldMetadata> fields;
        private SortedSet<ArtifactState> states;
    }

    static String toJson(ArtifactTypeMetadata metadata) {
        UUID parentTypeId = null;
        if (metadata.getParentMetadata() == null) {
            if (!metadata.getArtifactTypeId().equals(CORE_METADATA_TYPE_ID)) {
                throw new IllegalMetadataException("Tried to serialise an inconsistent type hierarchy.  All custom types must eventually inherit from core metadata");
            }
        }
        else {
            parentTypeId = metadata.getParentMetadata().getArtifactTypeId();
        }

        try {
            return objectMapper.writeValueAsString(
                StrippedDownArtifactTypeMetadata.builder()
                    .artifactTypeId(metadata.getArtifactTypeId())
                    .artifactTypeName(metadata.getArtifactTypeName())
                    .searchOptions(metadata.getSearchOptions())
                    .parentTypeId(parentTypeId)
                    .transactionTypes(new TreeSet<>(metadata.getTransactionTypes().values()))
                    .fields(new TreeSet<>(metadata.getFields().values()))
                    .states(new TreeSet<>(metadata.getStates().values()))
                    .build()
            );

        }
        catch (Exception e) {
            throw new IllegalMetadataException("Error while turning metadata into JSON", e);
        }
    }

    static ArtifactTypeMetadata fromCertificate(TransactionReader reader, ArtifactTypeMetadataLoader typeLoader) {
        // TODO: compress json
        //String json = decompressString(reader.getByteArray(ArtifactTypeType.TYPE_METADATA_JSON.getId()));
        String json = reader.getString(ArtifactTypeType.TYPE_METADATA_JSON.getId());
        UUID artifactId = reader.getArtifactId();
        Long timestamp = reader.getValidFrom();
        UUID artifactType = reader.getArtifactType();
        log.debug("{}[typeId:{}] artifactId:{}, fields: {}, timestamp: {}, json: {}", reader.getString(DISPLAY_NAME.getId()), artifactType,  artifactId, reader.getFields(), timestamp, json);
        return load(json, typeLoader);
    }

    static ArtifactTypeMetadata load(String json, ArtifactTypeMetadataLoader typeLoader) {
        try {
            //unmarshal the stripped down certs and feed the data to the builder in order to reconstruct the metadata
            StrippedDownArtifactTypeMetadata stored = objectMapper.readValue(json, StrippedDownArtifactTypeMetadata.class);
            ArtifactTypeMetadataBuilder builder = new ArtifactTypeMetadataBuilder(stored.getArtifactTypeId(), stored.getArtifactTypeName());

            if (stored.getParentTypeId() != null) {
                //la la la I'm ignoring the complexities of loading types in an inconvenient order la la la la la
                ArtifactTypeMetadata parentType = typeLoader.findByArtifactTypeId(stored.getParentTypeId()).get();
                builder.withParent(parentType);
            }

            stored.getTransactionTypes().forEach(builder::addTransactionType);
            stored.getFields().forEach(builder::addField);
            stored.getStates().forEach(builder::addState);
            builder.withSearchOptions(stored.getSearchOptions());

            return builder.getMetadata();
        }
        catch (Exception e) {
            throw new IllegalMetadataException("Error while reading metadata from JSON", e);
        }
    }

    public ArtifactTypeMetadata getMetadata() {
        return metadata;
    }

    public ArtifactTypeMetadataBuilder withParent(ArtifactTypeMetadata parentMetadata) {
        this.metadata.setParentMetadata(parentMetadata);
        return this;
    }

    public static ArtifactTypeMetadataBuilder extractMetadata(UUID artifactTypeId, String artifactTypeName, Class<?> metadataClass) {
        ArtifactTypeMetadataBuilder builder = new ArtifactTypeMetadataBuilder(artifactTypeId, artifactTypeName);

        Stream.of(metadataClass.getDeclaredFields()).forEach(field -> {
            try {
                Object value = field.get(null);
                if (value instanceof FieldMetadata) {
                    builder.addField((FieldMetadata) value);
                }
                else if (value instanceof TransactionType) {
                    builder.addTransactionType((TransactionType) value);
                }
                else if (value instanceof ArtifactState) {
                    builder.addState((ArtifactState) value);
                }
            }
            catch (IllegalMetadataException e) {
                throw e;
            }
            catch (Exception e) {
                //ignore - logger field etc
            }
        });
        return builder;
    }

    static byte[] compressString(String compressed) {
        byte[] bytes = compressed.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)){
            gz.write(bytes);
            gz.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BlockchainIOException("Error compressed string", e);
        }
    }

    static String decompressString(byte[] bytes) {
        try (GZIPInputStream gzin = new GZIPInputStream(new ByteArrayInputStream(bytes))){
            return new String(gzin.readAllBytes());
        } catch (IOException e) {
            throw new BlockchainIOException("Error reading compressed string", e);
        }
    }

    @Override
    public String toString() {
        return metadata.getArtifactTypeName();
    }

}
