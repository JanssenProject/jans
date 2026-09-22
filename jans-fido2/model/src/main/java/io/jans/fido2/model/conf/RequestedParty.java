/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.conf;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Supported RP
 *
 * @author Yuriy Movchan Date: 05/22/2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestedParty {

    private String id;

    private List<String> origins = new ArrayList<String>();

    /**
     * Per-RP assurance policy. Null means this RP has no policy of its own and every decision falls back
     * to the global configuration, which is how every relying party behaved before this field existed.
     */
    private RequestedPartyPolicy policy;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getOrigins() {
		return origins;
	}

	public void setOrigins(List<String> origins) {
		this.origins = origins;
	}

	public RequestedPartyPolicy getPolicy() {
		return policy;
	}

	public void setPolicy(RequestedPartyPolicy policy) {
		this.policy = policy;
	}
}
