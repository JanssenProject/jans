package io.jans.casa.core.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import java.util.List;

import io.jans.casa.misc.Utils;

@DataEntry
@ObjectClass("jansPerson")
public class PersonMobile extends BasePerson {

    @AttributeName(name = "jansMobileDevices")
    private String mobileDevices;

    @AttributeName
    private List<String> mobile;

    public String getMobileDevices(){
        return mobileDevices;
    }

    public List<String> getMobile() {
        return Utils.nonNullList(mobile);
    }

    public void setMobileDevices(String v) {
        this.mobileDevices = v;
    }

    public void setMobile(List<String> v) {
        this.mobile = v;
    }

}
