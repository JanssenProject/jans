/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.exception.fido.u2f;

import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;

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
