/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.exception.fido.u2f;

import java.util.Collections;
import java.util.List;

import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;

public class NoEligableDevicesException extends Exception {

	private static final long serialVersionUID = -7685552584573073454L;

	private final List<? extends DeviceRegistration> devices;

	public NoEligableDevicesException(List<? extends DeviceRegistration> devices, String message, Throwable cause) {
		super(message, cause);
		this.devices = Collections.unmodifiableList(devices);
	}

	public NoEligableDevicesException(List<? extends DeviceRegistration> devices, String message) {
		super(message);
		this.devices = Collections.unmodifiableList(devices);
	}

	public List<? extends DeviceRegistration> getDeviceRegistrations() {
		return devices;
	}

	public boolean hasDevices() {
		return !devices.isEmpty();
	}
}
