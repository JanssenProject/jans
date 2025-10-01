package io.jans.saml.metadata.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<IndexedEndpoint> getAssertionConsumerServices(SAMLBinding binding) {

        return assertionConsumerServices.stream()
            .filter((e) -> { return e.getBinding() == binding; })
            .collect(Collectors.toList());
    }

    public List<String> getAssertionConsumerServicesLocations(SAMLBinding binding) {

        return assertionConsumerServices.stream()
            .filter((e) -> { return e.getBinding() == binding;})
            .map((e) -> { return e.getLocation();})
            .collect(Collectors.toList());
    }

    public void addAssertionConsumerService(final IndexedEndpoint service) {

        this.assertionConsumerServices.add(service);
    }

    public void setAssertionConsumerServices(final List<IndexedEndpoint> services) {

        this.assertionConsumerServices = services;
    }
}