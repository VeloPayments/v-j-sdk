package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.cert.Certificate;
import com.velopayments.blockchain.crypt.SigningKeyPair;
import com.velopayments.blockchain.sdk.BlockchainUtils;
import com.velopayments.blockchain.sdk.TransactionReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactTypeMetadataTest {

    private static final ArtifactTypeMetadata CORE_METADATA = CoreMetadata.create().getMetadata();
    private static final FakeTypeLoader TYPE_LOADER = new FakeTypeLoader();

    public static class FakeTypeLoader implements ArtifactTypeMetadataLoader {
        @Override
        public Optional<ArtifactTypeMetadata> findByArtifactTypeId(UUID artifactTypeId) {
            if (artifactTypeId.equals(CoreMetadata.CORE_METADATA_TYPE_ID)) {
                return Optional.of(CORE_METADATA);
            }

            return Optional.empty();
        }
    }


    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void basicRoundTrip() {
        // given a metadata builder'
        UUID artifactTypeId = UUID.randomUUID();

        // when metadata is populated and a certificate generated'
        UUID transactionTypeId = UUID.randomUUID();
        Certificate certificate = new ArtifactTypeMetadataBuilder(artifactTypeId, "PAYEE")
            .addTransactionType(TransactionType.builder()
                .id(transactionTypeId)
                .name("CREATE_PAYEE")
                .build())
            .addField(FieldMetadata.builder()
                .id(1)
                .name("NAME")
                .type(FieldType.String)
                .build())
            .withParent(CORE_METADATA)
            .build()
            .sign(UUID.randomUUID(), SigningKeyPair.generate().getPrivateKey());

        // then the certificate has the right values'
        TransactionReader reader = new TransactionReader(certificate);
        assertThat(reader.getArtifactId()).isEqualTo(artifactTypeId);
        assertThat(reader.getTransactionType()).isEqualTo(ArtifactTypeType.TYPE_CREATED.getId());
        assertThat(reader.getArtifactType()).isEqualTo(ArtifactTypeType.ARTIFACT_TYPE_TYPE_TYPE_ID);
        assertThat(reader.getPreviousTransactionId()).isEqualTo(BlockchainUtils.INITIAL_TRANSACTION_UUID);

        // and the metadata can be reconstructed from the certificate'
        ArtifactTypeMetadata metadata = ArtifactTypeMetadata.fromCertificate(reader, TYPE_LOADER);
        assertThat(metadata.findTransactionTypeById(transactionTypeId)).hasValueSatisfying(txnType -> {
            assertThat(txnType.getName()).isEqualTo("CREATE_PAYEE");
        });
        assertThat(metadata.findFieldById(1)).hasValueSatisfying(txnType -> {
            assertThat(txnType.getName()).isEqualTo("NAME");
        });
    }

    @Test
    public void transactionTypes() {
        // given a metadata builder'
        ArtifactTypeMetadataBuilder builder = new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE").withParent(CORE_METADATA);
        // when transaction types are added'
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Certificate certificate = builder.addTransactionType(TransactionType.builder()
                .id(id1)
                .name("CREATE_PAYEE")
                .build())
            .addTransactionType(TransactionType.builder()
                .id(id2)
                .name("UPDATE")
                .build())
            .build(BlockchainUtils.INITIAL_TRANSACTION_UUID)
            .sign(UUID.randomUUID(), SigningKeyPair.generate().getPrivateKey());

        ArtifactTypeMetadata metadata = ArtifactTypeMetadata.fromCertificate(new TransactionReader(certificate), TYPE_LOADER);

        // then the transaction types are in the deserialised metadata'
        assertThat(metadata.findTransactionTypeById(id1)).hasValueSatisfying(txnType -> {
            assertThat(txnType.getName()).isEqualTo("CREATE_PAYEE");
        });
        assertThat(metadata.findTransactionTypeById(id2)).hasValueSatisfying(txnType -> {
            assertThat(txnType.getName()).isEqualTo("UPDATE");
        });

        assertThat(metadata.findTransactionTypeById(UUID.randomUUID())).isEmpty();
    }

    @Test
    public void searchOptions() {
        // given a metadata builder'
        Certificate certificate = new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .withParent(CORE_METADATA)
            .addField(FieldMetadata.builder()
                .id(1)
                .name("NAME")
                .type(FieldType.String)
                .includeInSearch(true)
                .build())
            .addField(FieldMetadata.builder()
                .id(2)
                .name("EMAIL")
                .type(FieldType.String)
                .includeInSearch(false)
                .build())
            .withSearchOptions(SearchOptions.FullTextNonEncryptedFieldValues)
            .build(BlockchainUtils.INITIAL_TRANSACTION_UUID)
            .sign(UUID.randomUUID(), SigningKeyPair.generate().getPrivateKey());

        ArtifactTypeMetadata metadata = ArtifactTypeMetadata.fromCertificate(new TransactionReader(certificate), TYPE_LOADER);

        // then the search options are preserve'
        assertThat(metadata.getSearchOptions()).isEqualTo(SearchOptions.FullTextNonEncryptedFieldValues);
        assertThat(metadata.findFieldByCamelCaseFieldName("email"))
            .hasValueSatisfying(field -> assertThat(field.isIncludeInSearch()).isFalse());
        assertThat(metadata.findFieldByCamelCaseFieldName("name"))
            .hasValueSatisfying(field -> assertThat(field.isIncludeInSearch()).isTrue());
    }


    @Test
    public void transactionTypesErrorCases_transactionIdWithNoUUID() {
        expectedEx.expect(IllegalMetadataException.class);
        expectedEx.expectMessage("id cannot be null");

        // when a transaction type is added with no UUID'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addTransactionType(TransactionType.builder()
                .name("CREATE_PAYEE")
                .build());
    }

    @Test
    public void transactionTypesErrorCases_spaceInTransactionType() {
        expectedEx.expect(IllegalMetadataException.class);
        expectedEx.expectMessage("must only contain upper case alphanumerics or underscores");

        // when a transaction type is added with a space in the name'
        createSimpleTypeBuilderWIthName("CREATE PAYEE");
    }

    @Test
    public void transactionTypesErrorCases_nullTransactionTypeName() {
        expectedEx.expect(IllegalMetadataException.class);

        // when a transaction type is added with a space in the name'
        createSimpleTypeBuilderWIthName(null);
    }

    @Test
    public void transactionTypesErrorCases_emptyTransactionTypeName() {
        expectedEx.expect(IllegalMetadataException.class);

        // when a transaction type is added with an empty string in the name'
        createSimpleTypeBuilderWIthName("");
    }

    @Test
    public void transactionTypesErrorCases_lowerCaseNameTransactionTypeName() {
        expectedEx.expect(IllegalMetadataException.class);

        // when a transaction type is added with a name not in upper case'
        createSimpleTypeBuilderWIthName("createPayee");
    }

    @Test
    public void transactionTypesErrorCases_symbolInTransactionTypeName() {
        expectedEx.expect(IllegalMetadataException.class);

        // when a transaction type is added with a symbol in the name'
        createSimpleTypeBuilderWIthName("CREATE_PAYEE_&");
    }

    @Test
    public void transactionTypesErrorCases_duplicateIdTypeName() {
        expectedEx.expect(DuplicateMetadataException.class);

        // when a transaction type is added with a duplicate id'
        UUID id = UUID.randomUUID();
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addTransactionType(TransactionType.builder()
                .id(id)
                .name("BOB")
                .build())
            .addTransactionType(TransactionType.builder()
                .id(id)
                .name("BOB2")
                .build());
    }

    @Test
    public void fields() {
        // when field metadata is added'
        Certificate certificate = new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
                .withParent(CORE_METADATA)
            .addField(FieldMetadata.builder()
                .id(1)
                .name("FIRST_NAME")
                .type(FieldType.String)
                .build())
            .addField(FieldMetadata.builder()
                .id(2)
                .name("LAST_NAME")
                .type(FieldType.String)
                .build())
            .build(BlockchainUtils.INITIAL_TRANSACTION_UUID)
            .sign(UUID.randomUUID(), SigningKeyPair.generate().getPrivateKey());

        ArtifactTypeMetadata metadata = ArtifactTypeMetadata.fromCertificate(new TransactionReader(certificate), TYPE_LOADER);


        // then the field types are in the deserialised metadata'
        assertThat(metadata.findFieldById(1))
            .hasValueSatisfying(field -> assertThat(field.getName()).isEqualTo("FIRST_NAME"));
        assertThat(metadata.findFieldById(2))
            .hasValueSatisfying(field -> assertThat(field.getName()).isEqualTo("LAST_NAME"));
        assertThat(metadata.findFieldById(3)).isEmpty();
    }

    private void createSimpleTypeBuilderWIthName(String name) {
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addTransactionType(TransactionType.builder()
                .id(UUID.randomUUID())
                .name(name)
                .build());
    }

    @Test
    public void artifactIdValidation() {
        expectedEx.expect(IllegalMetadataException.class);
        // when a metadata builder with a null id'
        new ArtifactTypeMetadataBuilder(null, "PAYEE");
    }

    @Test
    public void artifactEmptyNameValidation() {
        expectedEx.expect(IllegalMetadataException.class);
        // when a metadata builder with an empty name'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "");
    }

    @Test
    public void artifactLowerCaseNameValidation() {
        expectedEx.expect(IllegalMetadataException.class);
        // when a metadata builder with an empty name'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "payee");
    }

    @Test
    public void addFieldValidation_ZeroId() {
        expectedEx.expect(IllegalMetadataException.class);

        // when field metadata has zero id'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addField(FieldMetadata.builder()
                .id(0)
                .name("FIRST_NAME")
                .build());
    }

    @Test
    public void addFieldValidation_NoId() {
        expectedEx.expect(IllegalMetadataException.class);

        // when field metadata has no id'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addField(FieldMetadata.builder()
                .name("FIRST_NAME")
                .build());
    }

    @Test
    public void addFieldValidation_EmptyName() {
        expectedEx.expect(IllegalMetadataException.class);

        // when field metadata has empty name'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addField(FieldMetadata.builder()
                .id(1)
                .name("")
                .build());
    }

    @Test
    public void addFieldValidation_NullName() {
        expectedEx.expect(IllegalMetadataException.class);

        // when field metadata has empty name'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addField(FieldMetadata.builder()
                .id(1)
                .name(null)
                .build());
    }

    @Test
    public void addFieldValidation_DuplicateId() {
        expectedEx.expect(DuplicateMetadataException.class);

        // when field metadata has empty name'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addField(FieldMetadata.builder()
                .id(1)
                .name("ONE")
                .type(FieldType.String)
                .build())
            .addField(FieldMetadata.builder()
                .id(1)
                .name("TWO")
                .type(FieldType.String)
                .build());
    }

    @Test
    public void addStateValidation_zeroValue() {
        expectedEx.expect(IllegalMetadataException.class);

        // when field metadata has empty name'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addState(0, "NEW");
    }

    @Test
    public void addStateValidation_defaultValue() {
        expectedEx.expect(IllegalMetadataException.class);

        // when field metadata has empty name'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addState(ArtifactState.builder().name("NEW").build());
    }

    @Test
    public void addStateValidation_emptyName() {
        expectedEx.expect(IllegalMetadataException.class);

        // when field metadata has empty name'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addState(1, "");
    }

    @Test
    public void addStateValidation_nullName() {
        expectedEx.expect(IllegalMetadataException.class);

        // when field metadata has empty name'
        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addState(1, null);
    }

    @Test
    public void addStateValidation_duplicateId() {
        expectedEx.expect(DuplicateMetadataException.class);

        new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addState(1, "ONE")
            .addState(1, "TWO");
    }

    @Test
    public void addState() {
        // given a metadata builder'
        Certificate certificate = new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE").withParent(CORE_METADATA)
            .addState( 1, "STATE_ONE")
            .addState( 2, "STATE_TWO")
            .build(BlockchainUtils.INITIAL_TRANSACTION_UUID)
            .sign(UUID.randomUUID(), SigningKeyPair.generate().getPrivateKey());

        ArtifactTypeMetadata metadata = ArtifactTypeMetadata.fromCertificate(new TransactionReader(certificate), TYPE_LOADER);

        assertThat(metadata.findStateById(1)).hasValueSatisfying(state -> {
            assertThat(state.getName()).isEqualTo("STATE_ONE");
        });
        assertThat(metadata.findStateById(2)).hasValueSatisfying(state -> {
            assertThat(state.getName()).isEqualTo("STATE_TWO");
        });
        assertThat(metadata.findStateById(3)).isEmpty();

        assertThat(metadata.findStateByName("STATE_ONE")).hasValueSatisfying(state -> {
            assertThat(state.getValue()).isEqualTo(1);
        });
        assertThat(metadata.findStateByName("STATE_TWO")).hasValueSatisfying(state -> {
            assertThat(state.getValue()).isEqualTo(2);
        });
        assertThat(metadata.findStateByName("STATE_THREE")).isEmpty();
    }

    @Test
    public void baseMetadata() {
        // given parent and child metadata'
        UUID baseTxId = UUID.randomUUID();
        UUID childTxId = UUID.randomUUID();
        ArtifactTypeMetadata base = new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "BASE_OBJECT")
            .addTransactionType(baseTxId, "BASE_TX")
            .addField(1, "BASE_FIELD", FieldType.String)
            .getMetadata();

        ArtifactTypeMetadata child = new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .addTransactionType(childTxId, "CHILD_TX")
            .addField(2, "CHILD_FIELD", FieldType.String)
            .withParent(base)
            .getMetadata();

        // when the child is asked for a transaction type'
        // then it can be resolved via the parent linkage'
        assertThat(child.findTransactionTypeById(baseTxId)).hasValueSatisfying(ttype -> {
            assertThat(ttype.getName()).isEqualTo("BASE_TX");
        });
        assertThat(child.findTransactionTypeById(childTxId)).hasValueSatisfying(ttype -> {
            assertThat(ttype.getName()).isEqualTo("CHILD_TX");
        });
        assertThat(child.findTransactionTypeById(UUID.randomUUID())).isEmpty();

        assertThat(child.findFieldById(1)).hasValueSatisfying(field -> {
            assertThat(field.getName()).isEqualTo("BASE_FIELD");
        });
        assertThat(child.findFieldById(2)).hasValueSatisfying(field -> {
            assertThat(field.getName()).isEqualTo("CHILD_FIELD");
        });
        assertThat(child.findFieldById(3)).isEmpty();

        // when the child is asked for fields using camel case'
        // then the fields can be found via the parent type'
        assertThat(child.findFieldByCamelCaseFieldName("baseField")).hasValueSatisfying(field -> {
            assertThat(field.getId()).isEqualTo(1);
        });
        assertThat(child.findFieldByCamelCaseFieldName("childField")).hasValueSatisfying(field -> {
            assertThat(field.getId()).isEqualTo(2);
        });
        assertThat(child.findFieldByCamelCaseFieldName("blah")).isEmpty();
    }

    @Test
    public void metadataMustInheritFromCoreType_invalid() {
        // then IllegalStateException should be thrown because the type hierarchy is inconsistent'
        expectedEx.expect(IllegalMetadataException.class);

        // given parent and child metadata'
        ArtifactTypeMetadataBuilder builder = new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE");

        // when the builder is turned to JSON'
        ArtifactTypeMetadataBuilder.toJson(builder.getMetadata());
    }

    @Test
    public void metadataMustInheritFromCoreType() {
        // given parent and child metadata'
        // when a parent type is linked and the builder is turned to JSON'
        ArtifactTypeMetadataBuilder builder = new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .withParent(CORE_METADATA);
        String json = ArtifactTypeMetadataBuilder.toJson(builder.getMetadata());
        assertThat(json).isNotBlank();
    }

    @Test
    public void roundTrippingCalculatedFieldConfig() {
        // given a metadata builder with a calculated field'
        FieldMetadata PAYEE_LAST_NAME = new FieldMetadata(0x0414, "PAYEE_LAST_NAME", FieldType.String, false, false, 110);
        FieldMetadata PAYEE_FIRST_NAME = new FieldMetadata(0x0415, "PAYEE_FIRST_NAME", FieldType.String, false, false, 120);
        FieldMetadata DISPLAY_NAME = CoreMetadata.DISPLAY_NAME.copy().setCalculatedValue(Arrays.asList(
                new FieldValueSource(PAYEE_FIRST_NAME.getId()),
                new StaticValueSource(" "),
                new FieldValueSource(PAYEE_LAST_NAME.getId())));

        Certificate certificate = new ArtifactTypeMetadataBuilder(UUID.randomUUID(), "PAYEE")
            .withParent(CORE_METADATA)
            .addField(PAYEE_FIRST_NAME)
            .addField(PAYEE_LAST_NAME)
            .addField(DISPLAY_NAME)
            .build(BlockchainUtils.INITIAL_TRANSACTION_UUID)
            .sign(UUID.randomUUID(), SigningKeyPair.generate().getPrivateKey());

        ArtifactTypeMetadata metadata = ArtifactTypeMetadata.fromCertificate(new TransactionReader(certificate), TYPE_LOADER);

        // then the fields are still there'
        assertThat(metadata.findFieldByCamelCaseFieldName("displayName")).hasValueSatisfying(field -> {
            assertThat(field.getCalculatedValue()).containsExactly(
                new FieldValueSource(PAYEE_FIRST_NAME.getId()),
                new StaticValueSource(" "),
                new FieldValueSource(PAYEE_LAST_NAME.getId())
            );
        });
    }

    @Test
    public void badlyformedmetadatawillerroruponcreation() {
        // given Badly formed Artifact metadata'
        expectedEx.expect(DuplicateMetadataException.class);

        // when when create is called'
        TestEntityMetadata.create();
    }

    public static class TestEntityMetadata {
        public static ArtifactTypeMetadataBuilder create() {
            return ArtifactTypeMetadataBuilder.extractMetadata(UUID.randomUUID(), "TEST", TestEntityMetadata.class);
        }

        public static final FieldMetadata USER_NAME = new FieldMetadata(0x0421, "USER_NAME", FieldType.String, false, false, 330);
        public static final FieldMetadata USER_NAME_2 = new FieldMetadata(0x0421, "USER_NAME_2", FieldType.String, false, false, 330);
    }
}
