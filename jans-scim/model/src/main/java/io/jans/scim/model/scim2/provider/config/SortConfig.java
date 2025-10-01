/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.provider.config;

import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.AttributeDefinition;

/**
 * This class represents the <code>sort</code> complex attribute in the Service Provider Config (see section 5 of
 * RFC 7643).
 */
/*
 * Updated by jgomer on 2017-10-21
 */
public class SortConfig {

    @Attribute(description = "A Boolean value specifying whether or not sorting is supported.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.BOOLEAN)
	private boolean supported;

    /**
     * Creates an instance of SortConfig with all its fields unassigned.
     */
    public SortConfig(){ }

	/**
     * Creates an instance of SortConfig using the parameter values passed.
	 * @param supported Specifies whether sorting is supported.
	 */
	public SortConfig(boolean supported) {
		this.supported = supported;
	}

	/**
	 * Indicates whether sorting is supported.
	 * @return A boolean value
	 */
	public boolean isSupported() {
		return supported;
	}

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

}
