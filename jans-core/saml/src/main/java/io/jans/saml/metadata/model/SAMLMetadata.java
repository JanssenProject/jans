package io.jans.saml.metadata.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SAMLMetadata {

    private List<EntityDescriptor> entityDescriptors;

    public SAMLMetadata(List<EntityDescriptor> entityDescriptors) {

        this.entityDescriptors = entityDescriptors;
    }

    public final List<EntityDescriptor> getEntityDescriptors() {

        return this.entityDescriptors;
    }

    
    public final EntityDescriptor getEntityDescriptorByEntityId(final String entityId) {

        Optional<EntityDescriptor> ret =  entityDescriptors.stream()
            .filter((e) -> {return e.getEntityId().equals(entityId);})
            .findFirst();
        
        return (ret.isEmpty()? null : ret.get());
    }

    public final List<String> getEntityDescriptorIds() {

        return entityDescriptors.stream()
            .map((e) -> {return e.getEntityId();})
            .collect(Collectors.toList());
    }
}