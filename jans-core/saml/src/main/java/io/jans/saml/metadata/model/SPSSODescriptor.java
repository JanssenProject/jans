package io.jans.saml.metadata.model;

import java.util.ArrayList;
import java.util.List;

public class SPSSODescriptor extends SSODescriptor {

    private Boolean authnRequestsSigned;
    private Boolean wantAssertionsSigned;
    private List<IndexedEndpoint> assertionConsumerServices;

    public SPSSODescriptor() {

        this.authnRequestsSigned = false;
        this.wantAssertionsSigned = false;
        this.assertionConsumerServices = new ArrayList<>();
    }

    public void setAuthnRequestsSigned(final Boolean authnRequestsSigned) {

        this.authnRequestsSigned = authnRequestsSigned;
    }

    public Boolean getAuthnRequestsSigned() {

        return this.authnRequestsSigned;
    }

    public void setWantAssertionsSigned(final Boolean wantAssertionsSigned) {

        this.wantAssertionsSigned = wantAssertionsSigned;
    }

    public Boolean getWantAssertionsSigned() {

        return this.wantAssertionsSigned;
    }

    public List<IndexedEndpoint> getAssertionConsumerServices() {

        return this.assertionConsumerServices;
    }

    public void addAssertionConsumerService(final IndexedEndpoint service) {

        this.assertionConsumerServices.add(service);
    }

    public void setAssertionConsumerServices(final List<IndexedEndpoint> services) {

        this.assertionConsumerServices = services;
    }
}