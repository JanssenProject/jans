package io.jans.casa.plugins.emailotp.model;

import java.util.Objects;

import io.jans.casa.core.model.BasePerson;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass("jansPerson")
public class EmailPerson extends BasePerson {

    /**
     * 
     */
    private static final long serialVersionUID = -3072709087880306209L;
    
    @AttributeName(name = "mail")
    private String mail;

    @AttributeName(name = "jansEmail")
    private String jansEmail;
    
    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getJansEmail() {
        return jansEmail;
    }

    public void setJansEmail(String jansEmail) {
        this.jansEmail = jansEmail;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EmailPerson emailPersonObj = (EmailPerson) obj;
        return Objects.equals(this.mail, emailPersonObj.mail) &&
                Objects.equals(this.jansEmail, emailPersonObj.jansEmail);
    }

    @Override
    public int hashCode() {
      return Objects.hash(mail, jansEmail);
    }

}
