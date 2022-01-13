/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model;

import java.util.ArrayList;

public class OTPDevice {

	private ArrayList<Device> devices;

	public ArrayList<Device> getDevices() {
		return this.devices;
	}

	public void setDevices(ArrayList<Device> devices) {
		this.devices = devices;
	}

}
