package io.jans.saml.metadata.builder;

import java.util.List;

import io.jans.saml.metadata.model.Endpoint;
import io.jans.saml.metadata.model.SSODescriptor;

public abstract class SSODescriptorBuilder extends RoleDescriptorBuilder {

    protected SSODescriptorBuilder(final SSODescriptor descriptor) {
        super(descriptor);
    }

    public SSODescriptorBuilder nameIDFormats(final List<String> nameIDFormats) {

        ssoDescriptor().setNameIDFormats(nameIDFormats);
        return this;
    }

    public EndpointBuilder singleLogoutService() {

        Endpoint endpoint = new Endpoint();
        ssoDescriptor().addSingleLogoutService(endpoint);
        return new EndpointBuilder(endpoint);
    }

    private SSODescriptor ssoDescriptor() {

        return (SSODescriptor) this.descriptor;
    }
}