package io.jans.saml.metadata.builder;

import io.jans.saml.metadata.model.EntityDescriptor;
import io.jans.saml.metadata.model.SAMLMetadata;

import java.util.ArrayList;
import java.util.List;

public class SAMLMetadataBuilder {

    private List<EntityDescriptor> entityDescriptors;

    public SAMLMetadataBuilder() {

        this.entityDescriptors = new ArrayList<>();
    }
    
    public EntityDescriptorBuilder entityDescriptor() {

        EntityDescriptor descriptor = new EntityDescriptor();
        entityDescriptors.add(descriptor);
        return new EntityDescriptorBuilder(descriptor);
    }

    public SAMLMetadata build() {

        return new SAMLMetadata(entityDescriptors);
    }
}