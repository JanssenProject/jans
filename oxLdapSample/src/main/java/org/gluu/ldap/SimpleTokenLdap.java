package org.gluu.ldap;

import org.gluu.site.ldap.persistence.annotation.*;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.util.StringHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by eugeniuparvan on 1/12/17.
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthToken"})
public class SimpleTokenLdap implements Serializable {

    @LdapAttributesList(name = "name", value = "values", sortByName = true)
    protected List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();
    @LdapDN
    private String dn;
    @LdapCustomObjectClass
    private String[] customObjectClasses;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public List<CustomAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public String getAttribute(String ldapAttribute) {
        String attribute = null;
        if (ldapAttribute != null && !ldapAttribute.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (customAttribute.getName().equals(ldapAttribute)) {
                    attribute = customAttribute.getValue();
                    break;
                }
            }
        }

        return attribute;
    }

    public List<String> getAttributeValues(String ldapAttribute) {
        List<String> values = null;
        if (ldapAttribute != null && !ldapAttribute.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(customAttribute.getName(), ldapAttribute)) {
                    values = customAttribute.getValues();
                    break;
                }
            }
        }

        return values;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

}
