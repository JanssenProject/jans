/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.provider.config;

import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.AttributeDefinition;

/**
 * This class represents the <code>filter</code> complex attribute in the Service Provider Config (see section 5 of RFC 7643).
 */
/*
 * Updated by jgomer on 2017-10-21
 */
public class FilterConfig {

    @Attribute(description = "A Boolean value specifying whether or not the operation is supported",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.BOOLEAN)
	private boolean supported;

    @Attribute(description = "An integer value specifying the maximum number of resources returned in a response.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.INTEGER)
	private long maxResults;

    /**
     * Creates an instance of FilterConfig with all its fields unassigned.
     */
    public FilterConfig(){ }

	/**
     * Creates an instance of FilterConfig using the parameter values passed.
	 * @param supported Specifies whether the filters are supported in searches.
	 */
	public FilterConfig(boolean supported) {
		this.supported = supported;
	}

	/**
	 * Indicates whether filters are supported in searches or not
	 * @return A boolean value.
	 */
	public boolean isSupported() {
		return supported;
	}

    /**
     * Returns the maximum number of results that may be returned as a response when sending queries to search endpoints.
     * @return A numeric value
     */
	public long getMaxResults() {
		return maxResults;
	}

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public void setMaxResults(long maxResults) {
        this.maxResults = maxResults;
    }

}
