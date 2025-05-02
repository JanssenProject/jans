/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2;

import io.jans.scim.model.scim2.annotations.*;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;
import io.jans.scim.model.scim2.user.UserResource;

/**
 * This class represents the common resource attribute "meta" that contains metadata about the resource being described.
 * See section 3.1 of RFC 7643.
 */
/*
 * Created by jgomer on 2017-09-04.
 *
 * Note: StoreReference annotations are used by FilterVisitor classes to convert SCIM filter queries into LDAP queries
 */
public class Meta {

    @Attribute(description = "The resource Type",
            isCaseExact = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String resourceType;

    @Attribute(description = "Date and time the resource was created",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.DATETIME)
    @StoreReference(resourceType = {UserResource.class, Fido2DeviceResource.class},
            refs = {"jansCreationTimestamp", "creationDate"})
    //For effects of filters we don't use "jansMetaCreated" but "jansCreationTimestamp" which has generalizedTime data type)
    private String created;

    @Attribute(description = "Date and time the resource was last modified",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.DATETIME)
    @StoreReference(resourceType = {UserResource.class}, refs={"updatedAt"})
    //For effects of filters we don't use "jansMetaLastMod" but "updatedAt" which has generalizedTime data type)
    private String lastModified;

    @Attribute(description = "The location (URI) of the resource",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    @StoreReference(ref = "jansMetaLocation")
    private String location;

    @Attribute(description = "The version of the resource",
            isCaseExact = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    @StoreReference(ref = "jansMetaVer")
    private String version;

    /**
     * Retrieves the meta "resourceType" sub-attribute
     * @return A string value
     */
    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Retrieves the meta "created" sub-attribute
     * @return A string value
     */
    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    /**
     * Retrieves the meta "lastModified" sub-attribute
     * @return A string value
     */
    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Retrieves the meta "location" sub-attribute
     * @return A string value
     */
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns the version of the resource being represented.
     * @return A string value (null if no version information is available).
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
