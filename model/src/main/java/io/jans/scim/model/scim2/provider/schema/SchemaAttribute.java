/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.provider.schema;

import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.annotations.Attribute;

import java.util.List;

/**
 * Represents a SCIM resource attribute and its subattributes if any.
 */
/*
 * Created by jgomer on 2017-10-13.
 */
public class SchemaAttribute {

    @Attribute(description = "The attribute's name",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String name;

    @Attribute(description = "The attribute's data type",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String type;

    @Attribute(description = "When an attribute is of type \"complex\", \"subAttributes\" defines a set of sub-attributes",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX,
            multiValueClass = SchemaAttribute.class)
    private List<SchemaAttribute> subAttributes;

    @Attribute(description = "A Boolean value indicating the attribute's plurality",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.BOOLEAN)
    private boolean multiValued;

    @Attribute(description = "The attribute's human-readable description",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String description;

    @Attribute(description = "A Boolean value that specifies whether or not the attribute is required",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.BOOLEAN)
    private boolean required;

    @Attribute(description = "A Boolean value that specifies whether or not the attribute is required",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private List<String> canonicalValues;

    @Attribute(description = "A Boolean value that specifies whether or not a string attribute is case sensitive",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.BOOLEAN)
    private boolean caseExact;

    @Attribute(description = "A single keyword indicating the circumstances under which the value of the attribute can be (re)defined",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String mutability;

    @Attribute(description = "A single keyword that indicates when an attribute and associated values are returned in " +
            "response to a GET request or in response to a PUT, POST, or PATCH request",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String returned;

    @Attribute(description = "A single keyword value that specifies how the service provider enforces uniqueness of attribute values",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String uniqueness;

    @Attribute(description = "A multi-valued array of JSON strings that indicate the SCIM resource types that may be referenced",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private List<String> referenceTypes;

    /**
     * Default no args constructor.
     */
    public SchemaAttribute(){ }

    /**
     * Creates a SchemaAttribute with the name passed as param.
     * @param name The name of the attribute being modeled
     */
    public SchemaAttribute(String name){
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<SchemaAttribute> getSubAttributes() {
        return subAttributes;
    }

    public void setSubAttributes(List<SchemaAttribute> subAttributes) {
        this.subAttributes = subAttributes;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<String> getCanonicalValues() {
        return canonicalValues;
    }

    public void setCanonicalValues(List<String> canonicalValues) {
        this.canonicalValues = canonicalValues;
    }

    public boolean isCaseExact() {
        return caseExact;
    }

    public void setCaseExact(boolean caseExact) {
        this.caseExact = caseExact;
    }

    public String getMutability() {
        return mutability;
    }

    public void setMutability(String mutability) {
        this.mutability = mutability;
    }

    public String getReturned() {
        return returned;
    }

    public void setReturned(String returned) {
        this.returned = returned;
    }

    public String getUniqueness() {
        return uniqueness;
    }

    public void setUniqueness(String uniqueness) {
        this.uniqueness = uniqueness;
    }

    public List<String> getReferenceTypes() {
        return referenceTypes;
    }

    public void setReferenceTypes(List<String> referenceTypes) {
        this.referenceTypes = referenceTypes;
    }

    /**
     * Indicates whether another SchemaAttribute object is equal to this one
     * @param other An object to compare this one against
     * @return True if <code>other</code> is a SchemaAttribute whose {@link #getName() name} is the same as this instance name
     */
    @Override
    public boolean equals(Object other){
        boolean eq=false;
        if (other instanceof SchemaAttribute)
            eq=((SchemaAttribute) other).getName().equals(name);

        return eq;
    }

}
