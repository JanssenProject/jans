/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.exception.fido.u2f;

import org.gluu.oxauth.model.fido.u2f.DeviceRegistration;

public class InvalidDeviceCounterException extends DeviceCompromisedException {

	private static final long serialVersionUID = -3393844723613998052L;

	public InvalidDeviceCounterException(DeviceRegistration registration) {
		super(registration, "The device's internal counter was was smaller than expected. It's possible that the device has been cloned!");
	}
}
