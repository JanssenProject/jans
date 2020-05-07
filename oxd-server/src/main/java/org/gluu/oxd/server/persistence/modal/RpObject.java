package org.gluu.oxd.server.persistence.modal;

import java.io.Serializable;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

@DataEntry
@ObjectClass("oxRp")
public class RpObject implements Serializable {

    @DN
    private String dn;
    @AttributeName(name = "id")
    private String id;
    @AttributeName(name = "data")
    private String data;

    public RpObject(String dn, String id, String data) {
        this.dn = dn;
        this.id = id;
        this.data = data;
    }

    public RpObject() {
    }

    public String getDn() {
        return this.dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String toString() {
        return "RpObject{dn='" + this.dn + '\'' +
                ", id='" + this.id + '\'' +
                ", data='" + this.data + '\'' + '}';
    }
}
