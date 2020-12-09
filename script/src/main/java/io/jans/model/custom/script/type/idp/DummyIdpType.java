/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.idp;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * Dummy implementation of interface IdpType
 *
 * @author Yuriy Movchan Date: 06/18/2020
 */
public class DummyIdpType implements IdpType {

	@Override
	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public int getApiVersion() {
		return 1;
	}

	@Override
	public boolean translateAttributes(Object context, Map<String, SimpleCustomProperty> configurationAttributes) {
		return false;
	}

	@Override
	public boolean updateAttributes(Object context, Map<String, SimpleCustomProperty> configurationAttributes) {
		return false;
	}

}
