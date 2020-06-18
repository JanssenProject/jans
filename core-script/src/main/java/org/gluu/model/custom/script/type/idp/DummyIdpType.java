/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */package org.gluu.model.custom.script.type.idp;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.CustomScript;
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
	public boolean updateAttributes(Object context, Map<String, SimpleCustomProperty> configurationAttributes) {
		return false;
	}

}
