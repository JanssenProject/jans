package io.jans.saml.metadata.builder;

import java.time.Duration;
import java.util.Date;
import java.util.Arrays;

import io.jans.saml.metadata.model.ContactPerson;
import io.jans.saml.metadata.model.KeyDescriptor;
import io.jans.saml.metadata.model.Organization;
import io.jans.saml.metadata.model.RoleDescriptor;

public abstract class RoleDescriptorBuilder {


    protected RoleDescriptor descriptor; 

    protected RoleDescriptorBuilder(final RoleDescriptor descriptor) {

        this.descriptor = descriptor;
    }

    public RoleDescriptorBuilder id(final String id) {

        this.descriptor.setId(id);
        return this;
    }

    public RoleDescriptorBuilder validUntil(final Date validUntil) {

        this.descriptor.setValidUntil(validUntil);
        return this;
    }

    public RoleDescriptorBuilder cacheDuration(final Duration cacheDuration) {

        this.descriptor.setCacheDuration(cacheDuration);
        return this;
    }

    public RoleDescriptorBuilder supportedProtocol(final String protocol) {

        this.descriptor.addSupportedProtocol(protocol);
        return this;
    }

    public RoleDescriptorBuilder supportedProtocols(final String protocols) {

        if(protocols == null || protocols.isEmpty()) {

            return this;
        }
        this.descriptor.setSupportedProtocols(Arrays.asList(protocols.split("\\s+")));
        return this;
    }

    public RoleDescriptorBuilder errorUrl(final String errorUrl) {

        this.descriptor.setErrorUrl(errorUrl);
        return this;
    }

    public KeyDescriptorBuilder keyDescriptor() {

        KeyDescriptor keydescriptor = new KeyDescriptor();
        this.descriptor.addKeyDescriptor(keydescriptor);
        return new KeyDescriptorBuilder(keydescriptor);
    }

    public OrganizationBuilder organization() {

        Organization organization = new Organization();
        this.descriptor.setOrganization(organization);
        return new OrganizationBuilder(organization);
    }

    public ContactPersonBuilder contactPerson() {

        ContactPerson contactperson = new ContactPerson();
        this.descriptor.addContact(contactperson);
        return new ContactPersonBuilder(contactperson);
    }
}