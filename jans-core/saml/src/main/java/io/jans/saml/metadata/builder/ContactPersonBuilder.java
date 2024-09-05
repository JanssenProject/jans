package io.jans.saml.metadata.builder;

import io.jans.saml.metadata.model.ContactPerson;

import java.util.List;

public class ContactPersonBuilder {

    private ContactPerson contactPerson;

    public ContactPersonBuilder(final ContactPerson contactPerson) {

        this.contactPerson = contactPerson;
    }

    public ContactPersonBuilder type(final String type) {

        this.contactPerson.setType(type);
        return this;
    }

    public ContactPersonBuilder company(final String company) {

        this.contactPerson.setCompany(company);
        return this;
    }


    public ContactPersonBuilder givenName(final String givenName) {

        this.contactPerson.setGivenName(givenName);
        return this;
    }

    public ContactPersonBuilder surName(final String surName) {

        this.contactPerson.setSurName(surName);
        return this;
    }

    public ContactPersonBuilder emailAddress(final String emailAddress) {

        this.contactPerson.addEmailAddress(emailAddress);
        return this;
    }

    public ContactPersonBuilder emailAddresses(final List<String> emailAddresses) {

        this.contactPerson.setEmailAddresses(emailAddresses);
        return this;
    }

    public ContactPersonBuilder telephoneNumber(final String telephoneNumber) {

        this.contactPerson.addTelephoneNumber(telephoneNumber);
        return this;
    }

    public ContactPersonBuilder telephoneNumbers(final List<String> telephoneNumbers) {

        this.contactPerson.setTelephoneNumbers(telephoneNumbers);
        return this;
    }
}