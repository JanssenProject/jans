package io.jans.saml.metadata.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.Duration;
import java.util.stream.Collectors;

public class RoleDescriptor {

    private String id;
    private Date validUntil;
    private Duration cacheDuration;
    private List<String> supportedProtocols;
    private String errorUrl;
    private List<KeyDescriptor> keyDescriptors;
    private Organization organization;
    private List<ContactPerson> contacts;

    public RoleDescriptor() {

        this.id = null;
        this.validUntil = null;
        this.cacheDuration = null;
        this.supportedProtocols = new ArrayList<>();
        this.errorUrl = null;
        this.keyDescriptors = new ArrayList<>();
        this.organization = null;
        this.contacts = new ArrayList<>();
    }

    public String getId() {

        return this.id;
    }

    public void setId(final String id) {

        this.id = id;
    }

    public Date getValidUntil() {

        return this.validUntil;
    }

    public void setValidUntil(final Date validUntil) {

        this.validUntil = validUntil;
    }

    public Duration getCacheDuration() {

        return this.cacheDuration;
    }

    public void setCacheDuration(final Duration cacheDuration) {

        this.cacheDuration = cacheDuration;
    }

    public String getErrorUrl() {

        return this.errorUrl;
    }

    public void setErrorUrl(final String errorUrl) {

        this.errorUrl = errorUrl;
    }


    public List<String> getSupportedProtocols() {

        return this.supportedProtocols;
    }

    public void addSupportedProtocol(final String protocol) {

        this.supportedProtocols.add(protocol);
    }

    public void setSupportedProtocols(final List<String> supportedProtocols) {

        this.supportedProtocols = supportedProtocols;
    }

    public List<KeyDescriptor> getKeyDescriptors() {


        return this.keyDescriptors;
    }

    public List<KeyDescriptor> getEncryptionKeys() {

        return keyDescriptors 
            .stream()
            .filter((k) -> { return k.isEncryptionKey();})
            .collect(Collectors.toList());
    }

    public List<KeyDescriptor> getSigningKeys() {

        return keyDescriptors
            .stream()
            .filter((k)-> { return k.isSigningKey();})
            .collect(Collectors.toList());
    }

    public void addKeyDescriptor(final KeyDescriptor keyDescriptor) {

        this.keyDescriptors.add(keyDescriptor);
    }

    public Organization getOrganization() {

        return this.organization;
    }

    public void setOrganization(final Organization organization) {

        this.organization = organization;
    }

    public List<ContactPerson> getContacts() {

        return this.contacts;
    }

    public void addContact(final ContactPerson contact) {

        this.contacts.add(contact);
    }
}