package io.jans.casa.core.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import java.util.List;

import io.jans.casa.misc.Utils;

@DataEntry
@ObjectClass("jansPerson")
public class Person extends BasePerson {

    @AttributeName
    private String givenName;

    @AttributeName(name = "sn")
    private String surname;

    @AttributeName(name = "jansEnrollmentCode")
    private String enrollmentCode;

    @AttributeName(name = "jansPreferredMethod")
    private String preferredMethod;

    @AttributeName(name = "role")
    private List<String> roles;

    @AttributeName
    private List<String> memberOf;

    public String getGivenName() {
        return givenName;
    }

    public String getSurname() {
        return surname;
    }

    public String getEnrollmentCode() {
        return enrollmentCode;
    }

    public List<String> getMemberOf() {
        return Utils.nonNullList(memberOf);
    }

    public List<String> getRoles() {
        return Utils.nonNullList(roles);
    }

    public String getPreferredMethod() {
        return preferredMethod;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setEnrollmentCode(String enrollmentCode) {
        this.enrollmentCode = enrollmentCode;
    }

    public void setMemberOf(List<String> memberOf) {
        this.memberOf = memberOf;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public void setPreferredMethod(String preferredMethod) {
        this.preferredMethod = preferredMethod;
    }

}
