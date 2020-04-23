/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.custom.script.type.ciba;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * Dummy implementation of interface EndUserNotificationType
 *
 * @author Milton BO Date: 04/22/2020
 */
public class DummyEndUserNotificationType implements EndUserNotificationType {

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
	public boolean notifyEndUser(Object context) {
		return false;
	}

}
