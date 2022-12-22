/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.exception;

import io.jans.fido2.model.u2f.DeviceRegistration;

public class DeviceCompromisedException extends Exception {

    private static final long serialVersionUID = -2098466708327419261L;

    private final DeviceRegistration registration;

    public DeviceCompromisedException(DeviceRegistration registration, String message, Throwable cause) {
        super(message, cause);
        this.registration = registration;
    }

    public DeviceCompromisedException(DeviceRegistration registration, String message) {
        super(message);
        this.registration = registration;
    }

    public DeviceRegistration getDeviceRegistration() {
        return registration;
    }
}
