/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.provider.resourcetypes;

import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.AttributeDefinition;

/**
 * A class that represents the <code>schemaExtensions</code> complex attribute of ResourceType Schema (see section 6 of
 * RFC 7643).
 * @author Val Pecaoco
 */
/*
 * Updated by jgomer2001 on 2017-09-23
 */
public class SchemaExtensionHolder {

    @Attribute(description = "The URI of an extended schema, e.g., \"urn:edu:2.0:Staff\". This MUST be equal to the \"id\" " +
            "attribute of a \"Schema\" resource.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String schema;

    @Attribute(description = "A Boolean value that specifies whether or not the schema extension is required for the resource type.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.BOOLEAN)
    private boolean required;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean getRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

}
