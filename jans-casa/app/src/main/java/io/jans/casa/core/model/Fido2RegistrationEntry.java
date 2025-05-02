package io.jans.casa.core.model;

import java.util.Date;

import io.jans.as.model.fido.u2f.protocol.DeviceData;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

//Using Fido2RegistrationEntry directly from fido2-model artifact does not work well!
@DataEntry
@ObjectClass(value = "jansFido2RegistrationEntry")
public class Fido2RegistrationEntry extends Entry {

	@AttributeName
	private String displayName;

	@AttributeName
	private Date creationDate;

	@AttributeName(name = "jansId")
	private String id;

	@AttributeName(name = "jansApp")
	private String application;

	@JsonObject
	@AttributeName(name = "jansRegistrationData", ignoreDuringUpdate = true)
	private Fido2RegistrationData registrationData;

	@AttributeName(name = "jansCounter", ignoreDuringUpdate = true)
	private int counter;

	@JsonObject
	@AttributeName(name = "jansDeviceData", ignoreDuringUpdate = true)
	private DeviceData deviceData;

	@JsonObject
	@AttributeName(name = "jansStatus", ignoreDuringUpdate = true)
	private String registrationStatus;

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Fido2RegistrationData getRegistrationData() {
		return registrationData;
	}

	public void setRegistrationData(Fido2RegistrationData registrationData) {
		this.registrationData = registrationData;
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public String getRegistrationStatus() {
		return registrationStatus;
	}

	public void setRegistrationStatus(String registrationStatus) {
		this.registrationStatus = registrationStatus;
	}

	public DeviceData getDeviceData() {
		return deviceData;
	}

	public void setDeviceData(DeviceData deviceData) {
		this.deviceData = deviceData;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

}
