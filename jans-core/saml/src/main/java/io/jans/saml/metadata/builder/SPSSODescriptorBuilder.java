package io.jans.saml.metadata.builder;

import io.jans.saml.metadata.model.IndexedEndpoint;
import io.jans.saml.metadata.model.SPSSODescriptor;

public class SPSSODescriptorBuilder extends SSODescriptorBuilder {

    public SPSSODescriptorBuilder(final SPSSODescriptor descriptor) {
        super(descriptor);
    }

    public SPSSODescriptorBuilder authnRequestsSigned(final Boolean authnRequestsSigned) {

        spssoDescriptor().setAuthnRequestsSigned(authnRequestsSigned);
        return this;
    }

    public SPSSODescriptorBuilder wantAssertionsSigned(final Boolean wantAssertionsSigned) {
        
        spssoDescriptor().setWantAssertionsSigned(wantAssertionsSigned);
        return this;
    }

    public IndexedEndpointBuilder assertionConsumerService() {

        IndexedEndpoint endpoint = new IndexedEndpoint();
        spssoDescriptor().addAssertionConsumerService(endpoint);
        return new IndexedEndpointBuilder(endpoint);
    }

    private final SPSSODescriptor spssoDescriptor() {

        return (SPSSODescriptor) descriptor;
    }
}