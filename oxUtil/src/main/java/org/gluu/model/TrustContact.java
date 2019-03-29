/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TrustContact implements java.io.Serializable {

    private static final long serialVersionUID = -3268590744030750954L;
    private String name;
    private String phone;
    private String mail;
    private String title;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof TrustContact);
        if (result) {
            TrustContact contact = (TrustContact) o;
            result &= contact.getName() == name || contact.getName().equals(name);
            result &= contact.getTitle() == title || contact.getTitle().equals(title);
            result &= contact.getMail() == mail || contact.getMail().equals(mail);
            result &= contact.getPhone() == phone || contact.getPhone().equals(phone);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
