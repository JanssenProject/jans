package io.jans.saml.metadata.model;

import java.util.ArrayList;
import java.util.List;


public class ContactPerson {

    private String type;
    private String company;
    private String givenName;
    private String surName;
    private List<String> emailAddresses;
    private List<String> telephoneNumbers;

    public ContactPerson() {

        this.type = null;
        this.company = null;
        this.surName = null;
        this.emailAddresses = new ArrayList<>();
        this.telephoneNumbers = new ArrayList<>();
    }

    public String getType() {

        return this.type;
    }

    public void setType(final String type) {

        this.type = type;
    }

    public String getCompany() {

        return this.company;
    }

    public void setCompany(final String company) {

        this.company = company;
    }

    public String getGivenName() {

        return this.givenName;
    }

    public void setGivenName(final String givenName) {

        this.givenName = givenName;
    }


    public String getSurName() {

        return this.surName;
    }

    public void setSurName(final String surName) {

        this.surName = surName;
    }

    public List<String> getEmailAddresses() {

        return this.emailAddresses;
    }

    public void addEmailAddress(final String emailAddress) {

        this.emailAddresses.add(emailAddress);
    }

    public void setEmailAddresses(final List<String> emailAddresses) {

        this.emailAddresses = emailAddresses;
    }

    public List<String> getTelephoneNumbers() {

        return this.telephoneNumbers;
    }

    public void addTelephoneNumber(final String telephoneNumber) {

        this.telephoneNumbers.add(telephoneNumber);
    }

    public void setTelephoneNumbers(final List<String> telephoneNumbers) {

        this.telephoneNumbers = telephoneNumbers;
    }

}