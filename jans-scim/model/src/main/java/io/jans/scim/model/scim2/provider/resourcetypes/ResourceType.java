/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.provider.resourcetypes;

import java.util.*;

import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.annotations.Schema;
import io.jans.scim.model.scim2.AttributeDefinition;

/**
 * This class is used to specify metadata about a resource type. It's the key class for representing the output of the
 * <code>/ResourceTypes</code> endpoint. For more about this resource type see RFC 7643, section 6.
 */
/*
 * Created by jgomer on 2017-09-24.
 */
@Schema(id="urn:ietf:params:scim:schemas:core:2.0:ResourceType", name="Resource Type", description = "Specifies the metadata about a resource")
public class ResourceType extends BaseScimResource {

    @Attribute(description = "The resource type name.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
	private String name;

    @Attribute(description = "The resource type's human-readable description",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String description;

    @Attribute(description = "The resource type's HTTP-addressable endpoint relative to the Base " +
            "URL of the service provider, e.g., \"Users\".",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
	private String endpoint;

    @Attribute(description = "The resource type's primary/base schema URI, e.g., \"urn:ietf:params:scim:schemas:core:2.0:User\"." +
            "This MUST be equal to the \"id\" attribute of the associated \"Schema\" resource",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
	private String schema;

    @Attribute(description = "A list of URIs of the resource type's schema extensions.",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            multiValueClass = SchemaExtensionHolder.class,
            type = AttributeDefinition.Type.COMPLEX)
	private List<SchemaExtensionHolder> schemaExtensions;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public List<SchemaExtensionHolder> getSchemaExtensions() {
		return schemaExtensions;
	}

	public void setSchemaExtensions(List<SchemaExtensionHolder> schemaExtensions) {
		this.schemaExtensions = schemaExtensions;
	}

}
