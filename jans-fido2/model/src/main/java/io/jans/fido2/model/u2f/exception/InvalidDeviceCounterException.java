/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.exception;

import io.jans.fido2.model.u2f.DeviceRegistration;

public class InvalidDeviceCounterException extends DeviceCompromisedException {

    private static final long serialVersionUID = -3393844723613998052L;
 
    public InvalidDeviceCounterException(DeviceRegistration registration) {
        super(registration, "The device's internal counter was was smaller than expected. It's possible that the device has been cloned!");
    }
}
