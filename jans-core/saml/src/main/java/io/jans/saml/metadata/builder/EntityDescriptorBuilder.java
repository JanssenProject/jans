package io.jans.saml.metadata.builder;

import java.util.Date;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.jans.saml.metadata.model.EntityDescriptor;
import io.jans.saml.metadata.model.SPSSODescriptor;

public class EntityDescriptorBuilder {

    private final EntityDescriptor entityDescriptor;

    public EntityDescriptorBuilder(final EntityDescriptor entityDescriptor) {

        this.entityDescriptor = entityDescriptor;
    }

    public EntityDescriptorBuilder id(final String id) {

        this.entityDescriptor.setId(id);
        return this;
    }

    public EntityDescriptorBuilder entityId(final String entityId) {

        this.entityDescriptor.setEntityId(entityId);
        return this;
    }

    public EntityDescriptorBuilder cacheDuration(final Duration cacheDuration) {

        this.entityDescriptor.setCacheDuration(cacheDuration);
        return this;
    }

    public EntityDescriptorBuilder validUntil(final Date validUntil) {

        this.entityDescriptor.setValidUntil(validUntil);
        return this;
    }

    public SPSSODescriptorBuilder SPSSODescriptor() {

        SPSSODescriptor spssodescriptor = new SPSSODescriptor();
        this.entityDescriptor.addSpssoDescriptor(spssodescriptor);
        return new SPSSODescriptorBuilder(spssodescriptor);
    }
} 