/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.provider.config;

import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.AttributeDefinition;

/**
 * A class used to specify ETag configuration options (see section 5 of RFC 7643).
 */
/*
 * Updated by jgomer on 2017-10-21
 */
public class ETagConfig {

    @Attribute(description = "A Boolean value specifying whether or not the operation is supported.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.BOOLEAN)
	private boolean supported;

    /**
     * Creates an instance of ETagConfig with all its fields unassigned.
     */
    public ETagConfig(){}

	/**
     * Creates an instance of ETagConfig using the parameter values passed.
	 * @param supported Whether the ETags are supported in the implementation.
	 */
	public ETagConfig(boolean supported) {
		this.supported = supported;
	}

	/**
	 * Indicates whether the ETags are supported.
	 * @return A boolean value.
	 */
	public boolean isSupported() {
		return supported;
	}

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

}
