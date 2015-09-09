/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class RegisterResourceOpResponse implements IOpResponse {

    @JsonProperty(value = "_id")
    private String id;
    @JsonProperty(value = "_rev")
    private String rev;

    public RegisterResourceOpResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String p_id) {
        id = p_id;
    }

    public String getRev() {
        return rev;
    }

    public void setRev(String p_rev) {
        rev = p_rev;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterResourceOpResponse");
        sb.append("{id='").append(id).append('\'');
        sb.append(", rev='").append(rev).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
