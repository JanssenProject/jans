/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package io.jans.link.service;

import java.util.List;

import io.jans.link.model.GluuCustomFidoDevice;

/**
 * @author Val Pecaoco
 */
public interface IFidoDeviceService {

	String getDnForFidoDevice(String userId, String id);

	GluuCustomFidoDevice getGluuCustomFidoDeviceById(String userId, String id);

	void updateGluuCustomFidoDevice(GluuCustomFidoDevice gluuCustomFidoDevice);

	void removeGluuCustomFidoDevice(GluuCustomFidoDevice gluuCustomFidoDevice);

	List<GluuCustomFidoDevice> searchFidoDevices(String userInum, String ... returnAttributes) throws Exception;
}
