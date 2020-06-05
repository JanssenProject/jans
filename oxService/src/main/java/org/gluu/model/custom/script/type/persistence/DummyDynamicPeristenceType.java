/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */package org.gluu.model.custom.script.type.persistence;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.CustomScript;
/**
 * Dummy implementation of interface DynamicPeristanceType
 *
 * @author Yuriy Movchan Date: 06/04/2020
 */
public class DummyDynamicPeristenceType implements PersistenceType {

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
	public void onBeforeCreate(Object context, Map<String, SimpleCustomProperty> configurationAttributes) {
	}

	@Override
	public void onAfterCreate(Object context, Map<String, SimpleCustomProperty> configurationAttributes) {
	}

	@Override
	public void onBeforeDestroy(Object context, Map<String, SimpleCustomProperty> configurationAttributes) {
	}

	@Override
	public void onAfterDestroy(Object context, Map<String, SimpleCustomProperty> configurationAttributes) {
	}

	@Override
	public String createHashedPassword(String credential) {
		return credential;
	}

	@Override
	public boolean compareHashedPasswords(String credential, String storedCredential) {
		return false;
	}

}
