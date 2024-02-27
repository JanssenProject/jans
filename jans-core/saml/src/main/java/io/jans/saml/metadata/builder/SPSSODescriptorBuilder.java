package io.jans.saml.metadata.builder;

import io.jans.saml.metadata.model.IndexedEndpoint;
import io.jans.saml.metadata.model.SPSSODescriptor;

import java.util.List;

public class SPSSODescriptorBuilder extends SSODescriptorBuilder {

    public SPSSODescriptorBuilder(final SPSSODescriptor descriptor) {
        super(descriptor);
    }

    public SPSSODescriptorBuilder authnRequestsSigned(final Boolean authnRequestsSigned) {

        SPSSODescriptor().setAuthnRequestsSigned(authnRequestsSigned);
        return this;
    }

    public SPSSODescriptorBuilder wantAssertionsSigned(final Boolean wantAssertionsSigned) {
        
        SPSSODescriptor().setWantAssertionsSigned(wantAssertionsSigned);
        return this;
    }

    public IndexedEndpointBuilder assersionConsumerService() {

        IndexedEndpoint endpoint = new IndexedEndpoint();
        SPSSODescriptor().addAssertionConsumerService(endpoint);
        return new IndexedEndpointBuilder(endpoint);
    }

    private final SPSSODescriptor SPSSODescriptor() {

        return (SPSSODescriptor) descriptor;
    }
}