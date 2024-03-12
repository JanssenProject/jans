package io.jans.saml.metadata.model;

import java.util.List;

public class SAMLMetadata {

    private List<EntityDescriptor> entityDescriptors;

    public SAMLMetadata(List<EntityDescriptor> entityDescriptors) {

        this.entityDescriptors = entityDescriptors;
    }

    public final List<EntityDescriptor> getEntityDescriptors() {

        return this.entityDescriptors;
    }
}