/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileConfiguration implements Serializable {
    
	private String name;
	private String signResponses;	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
	public String getSignResponses() {
		return signResponses;
	}

	public void setSignResponses(String signResponses) {
		this.signResponses = signResponses;
	}



}
