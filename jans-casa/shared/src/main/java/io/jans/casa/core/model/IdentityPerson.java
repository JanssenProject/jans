package io.jans.casa.core.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import java.util.List;

import io.jans.casa.misc.Utils;

/**
 * Extends {@link BasePerson} in order to manipulate attributes <code>userPassword</code> and <code>jansExtUid</code>.
 */
@DataEntry
@ObjectClass("jansPerson")
public class IdentityPerson extends BasePerson {

    @AttributeName(name = "userPassword")
    private String password;

    @AttributeName
    private List<String> jansExtUid;

    @AttributeName
    private List<String> jansUnlinkedExternalUids;

    public boolean hasPassword() {
        return Utils.isNotEmpty(password);
    }

    public String getPassword() {
        return password;
    }

    public List<String> getJansExtUid() {
        return Utils.nonNullList(jansExtUid);
    }

    public List<String> getJansUnlinkedExternalUids() {
        return Utils.nonNullList(jansUnlinkedExternalUids);
    }

    public void setPassword(String userPassword) {
        this.password = userPassword;
    }

    public void setJansExtUid(List<String> jansExtUid) {
        this.jansExtUid = jansExtUid;
    }

    public void setJansUnlinkedExternalUids(List<String> jansUnlinkedExternalUids) {
        this.jansUnlinkedExternalUids = jansUnlinkedExternalUids;
    }

}
