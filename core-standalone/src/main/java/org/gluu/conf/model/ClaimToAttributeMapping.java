/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

/**
 * CAS oxAuth application configuration
 * 
 * @author Yuriy Movchan
 * @version 0.1, 03/25/2016
 */
package org.gluu.conf.model;

import java.io.Serializable;

/**
 * Claim to attribute mapping
 * 
 * @author Yuriy Movchan
 * @version 0.1, 03/25/2016
 */
public class ClaimToAttributeMapping implements Serializable {

	private static final long serialVersionUID = 3450326508968717097L;

	private String claim;
	private String attribute;

	public String getClaim() {
		return claim;
	}

	public void setClaim(String claim) {
		this.claim = claim;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ClaimToAttributeMapping [claim=").append(claim).append(", attribute=").append(attribute).append("]");
		return builder.toString();
	}

}
