/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.persistence;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * Dummy implementation of interface DynamicPeristanceType
 *
 * @author Yuriy Movchan Date: 06/04/2020
 */
public class DummyPeristenceType implements PersistenceType {

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
	public void onAfterCreate(Object context, Map<String, SimpleCustomProperty> configurationAttributes) {
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
