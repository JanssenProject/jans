package io.jans.as.common.model.stat;


import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 */
@DataEntry
@ObjectClass(value = "jansStatEntry")
public class StatEntry implements Serializable {

    @DN
    private String dn;

    @AttributeName(name = "jansId")
    private String id;

    @AttributeName(name = "jansData")
    private String month;

    @AttributeName(name = "dat")
    private String userHllData;

    @AttributeName(name = "attr")
    @JsonObject
    private Stat stat;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserHllData() {
        return userHllData;
    }

    public void setUserHllData(String userHllData) {
        this.userHllData = userHllData;
    }

    public Stat getStat() {
        if (stat == null)
            stat = new Stat();
        return stat;
    }

    public void setStat(Stat stat) {
        this.stat = stat;
    }
}
