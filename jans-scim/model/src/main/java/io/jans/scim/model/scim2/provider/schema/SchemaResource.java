/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.provider.schema;

import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.annotations.Schema;

import java.util.List;

/**
 * A class used to represent a schema (that a given SCIM resource type adheres to). Unlike other core resources, this one
 * contains complex objects within a sub-attribute. See section 7 of RFC 7643.
 */
/*
 * Created by jgomer on 2017-10-13.
 */
@Schema(id="urn:ietf:params:scim:schemas:core:2.0:Schema", name="Schema", description = "See section 7 RFC 7643")
public class SchemaResource extends BaseScimResource {

    @Attribute(description = "The schema's human-readable name",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String name;

    @Attribute(description = "The schema's human-readable description",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String description;

    @Attribute(description = "A complex type that defines service provider attributes and their qualities",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX,
            multiValueClass = SchemaAttribute.class,
            isRequired = true)
    private List<SchemaAttribute> attributes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SchemaAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<SchemaAttribute> attributes) {
        this.attributes = attributes;
    }

}
