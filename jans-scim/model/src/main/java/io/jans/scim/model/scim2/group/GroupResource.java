/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.group;

import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.annotations.Schema;
import io.jans.scim.model.scim2.annotations.StoreReference;

import java.util.Set;

/**
 * Group SCIM resource. See section 4.2 in RFC 7643.
 */
/*
 * Created by jgomer on 2017-09-12.
 *
 * Notes: Property names (member names) MUST match exactly as in the spec, so do not change!. Other classes may depend on
 * this one via reflection. Do not add members whose names are already at io.jans.scim.model.scim2.BaseScimResource.
 * Annotations applied at every member resemble what the spec states
 */
@Schema(id="urn:ietf:params:scim:schemas:core:2.0:Group", name="Group", description="Group")
public class GroupResource extends BaseScimResource {

    @Attribute(description = "A human-readable name for the Group",
            isRequired = true)
    @StoreReference(ref = "displayName")
    private String displayName;

    @Attribute(description = "A list of members of the Group",
            multiValueClass = Member.class,
            type = AttributeDefinition.Type.COMPLEX)
    @StoreReference(ref = "member")
    private Set<Member> members;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<Member> getMembers() {
        return members;
    }

    public void setMembers(Set<Member> members) {
        this.members = members;
    }

}
