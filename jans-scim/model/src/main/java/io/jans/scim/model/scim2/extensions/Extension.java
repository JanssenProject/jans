/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.extensions;

import java.util.HashMap;
import java.util.Map;

/**
 * A class used to represent an extension applicable to a SCIM resource. See section 3.3 of RFC 7643.
 * <p>To actually set or get custom attribute values for a resource, use
 * {@link io.jans.scim.model.scim2.BaseScimResource#addCustomAttributes(io.jans.scim.model.scim2.CustomAttributes) BaseScimResource#addCustomAttributes}
 * and {@link io.jans.scim.model.scim2.BaseScimResource#getCustomAttributes(String) BaseScimResource#getCustomAttributes}
 * respectively in conjunction with the {@link io.jans.scim.model.scim2.CustomAttributes CustomAttributes} class.</p>
 */
/*
 * Updated by jgomer on 2017-09-29.
 */
public class Extension {

    private String urn;
    private String name;
    private String description;
    private Map<String, ExtensionField> fields=new HashMap<>();

    /**
     * Constructs an instance of Extension associated to the URN passed with an empty collection of fields and unassigned
     * name and description.
     * @param urn A string representing the urn that identifies uniquely this extension
     */
    public Extension(String urn){
        this.urn=urn;
    }

    public String getUrn() {
        return urn;
    }

    public Map<String, ExtensionField> getFields() {
        return fields;
    }

    public void setFields(Map<String, ExtensionField> fields) {
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
