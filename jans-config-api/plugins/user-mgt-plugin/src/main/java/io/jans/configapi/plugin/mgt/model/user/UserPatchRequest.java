package io.jans.configapi.plugin.mgt.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.orm.model.base.CustomObjectAttribute;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPatchRequest implements Serializable {
 
    private static final long serialVersionUID = 1L;

    private String jsonPatchString;
   
    private List<CustomObjectAttribute> customAttributes;

    public String getJsonPatchString() {
        return jsonPatchString;
    }

    public void setJsonPatchString(String jsonPatchString) {
        this.jsonPatchString = jsonPatchString;
    }

    public List<CustomObjectAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomObjectAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    @Override
    public String toString() {
        return "UserPatchRequest [jsonPatchString=" + jsonPatchString + ", customAttributes=" + customAttributes + "]";
    }
}
