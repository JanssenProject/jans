/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.provider.config;

import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.AttributeDefinition;

/**
 * A class that holds values representing the configuration options for SCIM BULK operation (see section 5 of RFC 7643).
 */
/*
 * Updated by jgomer on 2017-10-21
 */
public class BulkConfig {

    @Attribute(description = "A Boolean value specifying whether or not the operation is supported.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.BOOLEAN)
	private boolean supported;

    @Attribute(description = "An integer value specifying the maximum number of operations.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.INTEGER)
	private int maxOperations;

    @Attribute(description = " An integer value specifying the maximum payload size in bytes",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.INTEGER)
	private long maxPayloadSize;

    /**
     * Creates an instance of BulkConfig with all its fields unassigned.
     */
    public BulkConfig(){ }

	/**
	 * Creates a BulkConfig instance based on parameters supplied.
	 * @param supported Specifies whether the bulk operation is supported.
	 * @param maxOperations Specifies the maximum number of operations supported per bulk.
	 * @param maxPayloadSize Specifies the maximum payload size in bytes supported per bulk.
	 */
	public BulkConfig(boolean supported, int maxOperations, long maxPayloadSize) {
		this.supported = supported;
		this.maxOperations = maxOperations;
		this.maxPayloadSize = maxPayloadSize;
	}

	/**
	 * Indicates whether the PATCH operation is supported.
	 * @return A boolean value
	 */
	public boolean isSupported() {
		return supported;
	}

	/**
	 * Retrieves the maximum number of operations supported in a bulk.
	 * @return The maximum number of operations.
	 */
	public int getMaxOperations() {
		return maxOperations;
	}

	/**
	 * Retrieves the maximum payload size allowed in a bulk.
	 * @return The maximum payload size in bytes.
	 */
	public long getMaxPayloadSize() {
		return maxPayloadSize;
	}

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public void setMaxOperations(int maxOperations) {
        this.maxOperations = maxOperations;
    }

    public void setMaxPayloadSize(long maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

}
