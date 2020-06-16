package com.velopayments.blockchain.sdk.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)  //allows copy constructor https://github.com/rzwitserloot/lombok/issues/908#issuecomment-157161768
@NoArgsConstructor
@AllArgsConstructor
public class FieldMetadata implements Comparable<FieldMetadata> {

    /**
     * The certificate field id of this field
     */
    private int id;

    /**
     * Field name - must be upper case with underscores instead of spaces
     */
    private String name;

    /**
     * The type of this field
     */
    private FieldType type;

    /**
     * Should values be encrypted?
     */
    private boolean encrypted;

    /**
     * Should this field be displayed?
     */
    private boolean hidden;

    /**
     * When visualising certificates containing this field, what should be the display sort order?
     */
    private int sortOrder;

    /**
     * If the parent type {@link SearchOptions} are set and includeInSearch is true then this field should be indexed by the VeloChain server
     */
    private boolean includeInSearch;

    /**
     * For fields with a calculated value, list their {@link ValueSource}s here
     *
     * e.g. this defines calculatedName as the firstName + " " + lastName
     *     public static final FieldMetadata CALCULATED_NAME = new FieldMetadata(0x0900, "CALCULATED_NAME", FieldType.String)
     *         .setCalculatedValue(Arrays.asList(
     *             new FieldValueSource(PAYEE_FIRST_NAME.getId()),
     *             new StaticValueSource(" "),
     *             new FieldValueSource(PAYEE_LAST_NAME.getId())
     *         ));
     */
    private List<ValueSource> calculatedValue;

    public FieldMetadata(int id, String name, FieldType type) {
        this(id, name, type, false, false, 1000, false);
    }

    public FieldMetadata(int id, String name, FieldType type, boolean encrypted, boolean hidden, int sortOrder) {
        this(id, name, type, encrypted, hidden, sortOrder, false);
    }

    public FieldMetadata(int id, String name, FieldType type, boolean encrypted, boolean hidden, int sortOrder, boolean includeInSearch) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.encrypted = encrypted;
        this.hidden = hidden;
        this.sortOrder = sortOrder;
        this.includeInSearch = includeInSearch;
    }

    /**
     * Create a shallow copy
     * @return a new {@code FieldMetadata} copy
     */
    public FieldMetadata copy() {
        return this.toBuilder().build();
    }

    @Override
    public int compareTo(FieldMetadata other) {
        return Integer.compare(id, other.id);
    }

    public FieldMetadata setCalculatedValue(List<ValueSource> calculatedValue) {
        this.calculatedValue = calculatedValue;
        return this;
    }

    public FieldMetadata setIncludeInSearch(boolean includeInSearch) {
        this.includeInSearch = includeInSearch;
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
