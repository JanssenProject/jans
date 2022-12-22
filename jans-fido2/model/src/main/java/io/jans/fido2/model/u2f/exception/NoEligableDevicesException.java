/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.exception;

import io.jans.fido2.model.u2f.DeviceRegistration;

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
