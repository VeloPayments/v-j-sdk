package com.velopayments.blockchain.sdk.metadata;

import com.velopayments.blockchain.sdk.TransactionReader;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Data
@NoArgsConstructor
public class ArtifactTypeMetadata {

    /**
     * NOTE: If adding new data here, consider also adding it to StrippedDownArtifactTypeMetadata
     */
    private UUID artifactTypeId;
    private String artifactTypeName;
    private ArtifactTypeMetadata parentMetadata;

    private final Map<UUID,TransactionType> transactionTypes = new HashMap<>();

    private final Map<Integer, ArtifactState> states = new HashMap<>();
    private final Map<String, ArtifactState> statesByUpperCaseStateName = new HashMap<>();

    private final Map<Integer,FieldMetadata> fields = new HashMap<>();
    private final Map<String,FieldMetadata> fieldsByCamelCaseFieldName = new HashMap<>();

    /**
     * If the {@link SearchOptions} are set then fields with includeInSearch=true will be indexed by the VeloChain server
     */
    private SearchOptions searchOptions = SearchOptions.None;

    ArtifactTypeMetadata(UUID artifactTypeId, String artifactTypeName) {
        this.artifactTypeId = artifactTypeId;
        this.artifactTypeName = artifactTypeName;
    }

    /**
     * Get a {@link TransactionType} by id
     * @return null if the transaction type was not found
     */
    public Optional<TransactionType> findTransactionTypeById(UUID transactionTypeId) {
        Optional<TransactionType> transactionType = Optional.ofNullable(transactionTypes.get(transactionTypeId));
        if (transactionType.isEmpty() && parentMetadata != null) {
            transactionType = parentMetadata.findTransactionTypeById(transactionTypeId);
        }

        return transactionType;
    }

    /**
     * Get a {@link ArtifactState} by id
     *
     * @param stateId
     *
     * The type hierarchy (if any) will also be searched.
     *
     * @return null if the state was not found
     */
    public Optional<ArtifactState> findStateById(Integer stateId) {
        Optional<ArtifactState> stateMetadata = Optional.ofNullable(states.get(stateId));
        if (stateMetadata.isEmpty() && parentMetadata != null) {
            stateMetadata = parentMetadata.findStateById(stateId);
        }
        return stateMetadata;
    }


    /**
     * Find by the state name (in upper case)
     */
    public Optional<ArtifactState> findStateByName(String stateName) {
        Optional<ArtifactState> stateMetadata = Optional.ofNullable(statesByUpperCaseStateName.get(stateName));
        if (stateMetadata.isEmpty() && parentMetadata != null) {
            stateMetadata = parentMetadata.findStateByName(stateName);
        }
        return stateMetadata;
    }


    /**
     * Get a {@link FieldMetadata} by id
     *
     * @param fieldId
     *
     * The type hierarchy (if any) will also be searched.
     *
     * @return null if the field was not found
     */
    public Optional<FieldMetadata> findFieldById(Integer fieldId) {
        Optional<FieldMetadata> fieldMetadata = Optional.ofNullable(fields.get(fieldId));
        if (fieldMetadata.isEmpty() && parentMetadata != null) {
            fieldMetadata = parentMetadata.findFieldById(fieldId);
        }
        return fieldMetadata;
    }


    /**
     * Get a {@link FieldMetadata} by id
     *
     * @param camelCaseFieldName e.g. for a field FIRST_NAME, pass firstName
     *
     * The type hierarchy (if any) will also be searched.
     *
     * @return null if the field was not found
     */
    public Optional<FieldMetadata> findFieldByCamelCaseFieldName(String camelCaseFieldName) {
        Optional<FieldMetadata> fieldMetadata = Optional.ofNullable(fieldsByCamelCaseFieldName.get(camelCaseFieldName));
        if (fieldMetadata.isEmpty() && parentMetadata != null) {
            fieldMetadata = parentMetadata.findFieldByCamelCaseFieldName(camelCaseFieldName);
        }
        return fieldMetadata;
    }

    public static ArtifactTypeMetadata fromCertificate(TransactionReader transactionReader, ArtifactTypeMetadataLoader typeLoader) {
        return ArtifactTypeMetadataBuilder.fromCertificate(transactionReader, typeLoader);
    }

    public void addTransaction(TransactionType transaction) {
        if (transactionTypes.containsKey(transaction.getId())) {
            throw new DuplicateMetadataException(String.format("Transaction [%s] is a duplicate of transaction [%s]", transaction, transactionTypes.get(transaction.getId())));
        }
        transactionTypes.put(transaction.getId(), transaction);
    }

    public void addField(FieldMetadata field) {
        if (fields.containsKey(field.getId())) {
            throw new DuplicateMetadataException(String.format("Field [%s] is a duplicate of field [%s]", field, fields.get(field.getId())));
        }
        fields.put(field.getId(), field);

        String camelCase = CamelCase.upperUnderscoreToLowerCamelCase(field.getName());
        fieldsByCamelCaseFieldName.put(camelCase, field);
    }

    public void addState(ArtifactState state) {
        if (states.containsKey(state.getValue())) {
            throw new DuplicateMetadataException(String.format("State [%s] is a duplicate of state [%s]", state, states.get(state.getValue())));
        }

        states.put(state.getValue(), state);
        statesByUpperCaseStateName.put(state.getName(), state);
    }

}
