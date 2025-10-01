package io.jans.casa.core.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.InumEntry;

import java.util.List;
import java.util.Optional;

import io.jans.casa.misc.Utils;

@DataEntry
@ObjectClass("jansClnt")
public class Client extends InumEntry {

    @AttributeName
    private String displayName;

    @AttributeName
    private String jansClntURI;

    @AttributeName(name ="jansContact")
    private List<String> contacts;

    @AttributeName(name = "jansLogoURI")
    private String logoURI;

    @AttributeName(name = "jansPolicyURI")
    private String policyURI;

    @AttributeName(name = "jansTosURI")
    private String tosURI;

    @AttributeName
    private String jansClntIdIssuedAt;

    public String getJansClntIdIssuedAt() {
        return jansClntIdIssuedAt;
    }

    public void setJansClntIdIssuedAt(String jansClntIdIssuedAt) {
        this.jansClntIdIssuedAt = jansClntIdIssuedAt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getJansClntURI() {
        return jansClntURI;
    }

    public List<String> getContacts() {
        return Utils.nonNullList(contacts);
    }

    public String getLogoURI() {
        return logoURI;
    }

    public String getPolicyURI() {
        return policyURI;
    }

    public String getTosURI() {
        return tosURI;
    }

    /**
     * Constructs an ID based on inum value, dropping all non alphabetic chars
     * @return A string
     */
    public String getAlternativeID() {
        return Optional.ofNullable(getInum())
                .map(str -> str.replaceAll("\\W","")).orElse (null);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setJansClntURI(String jansClntURI) {
        this.jansClntURI = jansClntURI;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public void setLogoURI(String logoURI) {
        this.logoURI = logoURI;
    }

    public void setPolicyURI(String policyURI) {
        this.policyURI = policyURI;
    }

    public void setTosURI(String tosURI) {
        this.tosURI = tosURI;
    }

}
