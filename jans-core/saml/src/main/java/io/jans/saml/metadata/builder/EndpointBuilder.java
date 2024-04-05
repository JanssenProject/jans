package io.jans.saml.metadata.builder;

import io.jans.saml.metadata.model.SAMLBinding;
import io.jans.saml.metadata.model.Endpoint;

public class EndpointBuilder {

    protected Endpoint endpoint;

    public EndpointBuilder(final Endpoint endpoint) {

        this.endpoint = endpoint;
    }

    public EndpointBuilder binding(final SAMLBinding binding) {

        this.endpoint.setBinding(binding);
        return this;
    }

    public EndpointBuilder location(final String location) {

        this.endpoint.setLocation(location);
        return this;
    }

    public EndpointBuilder responseLocation(final String responseLocation) {

        this.endpoint.setResponseLocation(responseLocation);
        return this;
    }
}