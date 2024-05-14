package io.jans.saml.metadata.model;

import java.time.Duration;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;




public class EntityDescriptor {

    private String id;
    private String entityId;
    private Duration cacheDuration;
    private Date validUntil;
    private List<SPSSODescriptor> spssoDescriptors;

    public EntityDescriptor() {
        
        this.id = null;
        this.entityId = null;
        this.cacheDuration = null;
        this.validUntil = null;
        this.spssoDescriptors = new ArrayList<>();
    }

    public void setId(final String id) {

        this.id = id;
    }

    public String getId() {

        return this.id;
    }

    public void setEntityId(final String entityId) {

        this.entityId = entityId;
    }

    public String getEntityId() {

        return this.entityId;
    }

    public void setCacheDuration(Duration cacheDuration) {

        this.cacheDuration = cacheDuration;
    }

    public Duration getCacheDuration() {

        return this.cacheDuration;
    }

    public void setValidUntil(Date validUntil) {

        this.validUntil = validUntil;
    }

    public Date getValidUntil() {

        return this.validUntil;
    }

    public List<SPSSODescriptor> getSpssoDescriptors() {

        return this.spssoDescriptors;
    }

    public SPSSODescriptor getFirstSpssoDescriptor() {

        if(!this.spssoDescriptors.isEmpty()) {
            return this.spssoDescriptors.get(0);
        }
        return null;
    }

    public void addSpssoDescriptor(final SPSSODescriptor descriptor) {

        this.spssoDescriptors.add(descriptor);
    }
}