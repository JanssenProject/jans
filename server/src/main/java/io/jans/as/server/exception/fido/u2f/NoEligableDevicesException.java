/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.exception.fido.u2f;

import io.jans.as.server.model.fido.u2f.DeviceRegistration;

import java.util.Collections;
import java.util.List;

public class NoEligableDevicesException extends Exception {

	private static final long serialVersionUID = -7685552584573073454L;

	private final List<? extends DeviceRegistration> deviceRegistrations;

	public NoEligableDevicesException(List<? extends DeviceRegistration> deviceRegistrations, String message, Throwable cause) {
		super(message, cause);
		this.deviceRegistrations = Collections.unmodifiableList(deviceRegistrations);
	}

	public NoEligableDevicesException(List<? extends DeviceRegistration> deviceRegistrations, String message) {
		super(message);
		this.deviceRegistrations = Collections.unmodifiableList(deviceRegistrations);
	}

	public List<? extends DeviceRegistration> getDeviceRegistrations() {
		return deviceRegistrations;
	}

	public boolean hasDevices() {
		return !deviceRegistrations.isEmpty();
	}
}
