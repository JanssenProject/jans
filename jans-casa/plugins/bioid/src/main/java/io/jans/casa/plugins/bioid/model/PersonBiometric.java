package io.jans.casa.plugins.bioid.model;

import io.jans.casa.core.model.BasePerson;
import io.jans.persist.annotation.AttributeName;
import io.jans.persist.annotation.DataEntry;
import io.jans.persist.annotation.ObjectClass;

@DataEntry
@ObjectClass("gluuPerson")
public class PersonBiometric extends BasePerson {

    @AttributeName(name = "oxBiometricDevices")
    private String bioMetricDevices;

    @AttributeName
    private String bioid;

	public String getBioMetricDevices() {
		return bioMetricDevices;
	}

	public void setBioMetricDevices(String bioMetricDevices) {
		this.bioMetricDevices = bioMetricDevices;
	}

	public String getBioid() {
		return bioid;
	}

	public void setBioid(String bioid) {
		this.bioid = bioid;
	}

	

	

    

}
