/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f;

import java.io.Serializable;

/**
 * U2F Device registration with status
 *
 * @author Yuriy Movchan Date: 03/22/2016
 */
public class DeviceRegistrationResult implements Serializable {

    private static final long serialVersionUID = -1542131162244920584L;

    private DeviceRegistration deviceRegistration;

    private Status status;

    public DeviceRegistrationResult() {
    }

    public DeviceRegistrationResult(DeviceRegistration deviceRegistration, Status status) {
        this.deviceRegistration = deviceRegistration;
        this.status = status;
    }

    public DeviceRegistration getDeviceRegistration() {
        return deviceRegistration;
    }

    public void setDeviceRegistration(DeviceRegistration deviceRegistration) {
        this.deviceRegistration = deviceRegistration;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        APPROVED, CANCELED
    }

}
