/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2;

import io.jans.scim.model.scim2.annotations.*;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;
import io.jans.scim.model.scim2.fido.FidoDeviceResource;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.*;

/**
 * This class represents the root hierarchy of SCIM resources. All of them: user, group, etc. are subclasses of this class.
 */
/*
 * Notes: Property names (member names) MUST match exactly as in the spec, so do not change!. Add a new item to the list
 * found in the static block of code at io.jans.scim.model.scim2.util.IntrospectUtil when a new subclass (SCIM resource)
 * is added. StoreReference annotations are used to convert SCIM filter queries into database queries 
 */
public class BaseScimResource {

    @Attribute(description = "The schemas attribute is a REQUIRED attribute and is an array of Strings containing URIs " +
            "that are used to indicate the namespaces of the SCIM schemas that define the attributes present in the " +
            "current JSON structure",
            isRequired = true,
            //mutability = AttributeDefinition.Mutability.READ_ONLY,
            /* This should not be READ_ONLY as the spec says, ie. if upon creation only the default schema is provided and
               then via an update a custom attribute is specified, the schemas attributes needs to be updated! */
            returned = AttributeDefinition.Returned.ALWAYS)
    private Set<String> schemas;

    @Attribute(description = "A unique identifier for a SCIM resource as defined by the service provider",
            isRequired = false,     //Notice that clients don't need to pass it really
            isCaseExact = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            returned = AttributeDefinition.Returned.ALWAYS,
            uniqueness = AttributeDefinition.Uniqueness.SERVER)
    @StoreReference(resourceType = {UserResource.class, GroupResource.class, FidoDeviceResource.class, Fido2DeviceResource.class},
            refs = {"inum", "inum", "jansId", "jansId"})
    private String id;

    @Attribute(description = "A String that is an identifier for the resource as defined by the provisioning client",
            isCaseExact = true)
    @StoreReference(resourceType = {UserResource.class}, refs = {"jansExtId"})
    private String externalId;

    @Attribute(description = "A complex attribute containing resource metadata",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.COMPLEX)
    private Meta meta;

    private Map<String, Object> extendedAttrs=new HashMap<>();   //must never be null

    /**
     * Replaces the custom attributes belonging to the resource extension identified by the <code>uri</code> passed as
     * parameter with the attribute/value pairs supplied in the <code>Map</code>. Developers are highly encouraged not
     * to use this method but {@link #addCustomAttributes(CustomAttributes)} instead which adds type-safety.
     * <p>Note that this method does not apply any sort of validation. Whether the <code>uri</code> and attributes are
     * recognized or the values are consistent with data types registered in Gluu Server, is something that is performed
     * only when the resource is passed in a service method invocation.</p>
     * @param uri A string with URI that identifies an extension
     * @param map A Map holding attribute names (Strings) and values (Objects).
     */
    @JsonAnySetter
    public void addCustomAttributes(String uri, Map<String, Object> map) {
        extendedAttrs.put(uri, map);
        schemas.add(uri);
    }

    /**
     * Adds the custom attributes contained in the {@link CustomAttributes} instance passed to this method. All previously
     * added attributes are replaced if they are linked to the same <code>uri</code> that <code>customAttributes</code>
     * parameter is associated to.
     * <p>Note that this method does not apply any sort of validation. Whether the <code>uri</code> and attributes are
     * recognized or the values are consistent with data types registered in Gluu Server, is something that is performed
     * only when the resource is passed in a service method invocation.</p>
     * @param customAttributes An object that comprised of attribute/value pairs
     */
    public void addCustomAttributes(CustomAttributes customAttributes){
        addCustomAttributes(customAttributes.getUri(), customAttributes.getAttributeMap());
    }

    /**
     * Retrieves all custom attributes found in this resource object. The attributes are structured hierarchically in a
     * <code>Map</code> where they can be looked up using the <code>uri</code> to which the attributes belong to.
     * <p>Developers are highly encouraged not to use this method but {@link #getCustomAttributes(String)} instead
     * which adds type-safety.</p>
     * @return A Map with all custom attributes
     */
    @JsonAnyGetter
    public Map<String, Object> getCustomAttributes(){
        return extendedAttrs;
    }

    /**
     * Retrieves the custom attributes found in this resource object associated to the <code>uri</code> supplied.
     * @param uri A String value representing a URI
     * @return A {@link CustomAttributes} instance that allows developers to inspect attributes and values in a type-safe manner.
     */
    public CustomAttributes getCustomAttributes(String uri){
        if (extendedAttrs.get(uri)==null)
            return null;
        return new CustomAttributes(uri, IntrospectUtil.strObjMap(extendedAttrs.get(uri)));
    }

    /**
     * Constructs a basic SCIM resource with all its attributes unassigned
     */
    public BaseScimResource() {
        schemas = new HashSet<>();
        String defSchema = ScimResourceUtil.getDefaultSchemaUrn(getClass());
        if (defSchema != null) {
            schemas.add(defSchema);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas;
    }

}
