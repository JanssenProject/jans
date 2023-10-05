package io.jans.casa.core.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass("jansPerson")
public class PersonPreferences extends BasePerson {

    @AttributeName(name = "jansStrongAuthPolicy")
    private String strongAuthPolicy;

    @AttributeName(name = "jansTrustedDevices")
    private String trustedDevices;

    public String getStrongAuthPolicy() {
        return strongAuthPolicy;
    }

    public String getTrustedDevices() {
        return trustedDevices;
    }

    public void setStrongAuthPolicy(String strongAuthPolicy) {
        this.strongAuthPolicy = strongAuthPolicy;
    }

    public void setTrustedDevices(String trustedDevicesInfo) {
        this.trustedDevices = trustedDevicesInfo;
    }

}
