package io.jans.configapi.plugin.shibboleth.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttributeMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String jansAttribute;
    private String samlAttribute;
    private String samlAttributeOid;
    private String friendlyName;
    private String nameFormat;
    private boolean enabled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJansAttribute() {
        return jansAttribute;
    }

    public void setJansAttribute(String jansAttribute) {
        this.jansAttribute = jansAttribute;
    }

    public String getSamlAttribute() {
        return samlAttribute;
    }

    public void setSamlAttribute(String samlAttribute) {
        this.samlAttribute = samlAttribute;
    }

    public String getSamlAttributeOid() {
        return samlAttributeOid;
    }

    public void setSamlAttributeOid(String samlAttributeOid) {
        this.samlAttributeOid = samlAttributeOid;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "AttributeMapping{" +
                "id='" + id + '\'' +
                ", jansAttribute='" + jansAttribute + '\'' +
                ", samlAttribute='" + samlAttribute + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
