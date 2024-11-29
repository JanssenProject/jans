package io.jans.casa.plugins.emailotp.model;

import io.jans.casa.core.model.BasePerson;
import io.jans.orm.annotation.*;

import java.util.List;

@DataEntry
@ObjectClass("jansPerson")
public class EmailPerson extends BasePerson {

    @AttributeName(name = "mail")
    private List<String> emails;
   
    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

}
