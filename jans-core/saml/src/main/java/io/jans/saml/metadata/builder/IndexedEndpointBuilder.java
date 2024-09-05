package io.jans.saml.metadata.builder;

import io.jans.saml.metadata.model.IndexedEndpoint;

public class IndexedEndpointBuilder extends EndpointBuilder  {

    public IndexedEndpointBuilder(final IndexedEndpoint endpoint) {
        super(endpoint);
    }

    public IndexedEndpointBuilder index(final Integer index) {

        indexedEndpoint().setIndex(index);
        return this;
    }

    public IndexedEndpointBuilder isDefault(final Boolean isdefault) {

        indexedEndpoint().setIsDefault(isdefault);
        return this;
    }

    private IndexedEndpoint indexedEndpoint() {

        return (IndexedEndpoint) endpoint;
    }
}