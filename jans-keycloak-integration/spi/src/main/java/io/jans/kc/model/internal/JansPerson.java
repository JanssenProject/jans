package io.jans.kc.model.internal;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

import io.jans.orm.annotation.*;
import io.jans.orm.model.base.CustomObjectAttribute;

@DataEntry
@ObjectClass(value="jansPerson")
public class JansPerson implements Serializable {

    private static final long serialVersionUID = -1L;

    @DN
    private String dn;

    @AttributesList(name="name",value="values",multiValued="multiValued")
    private List<CustomObjectAttribute> customAttributes = new ArrayList<>();

    public String getDn() {

        return this.dn;
    }

    public void setDn(final String dn) {
        
        this.dn = dn;
    }

    public void setCustomAttributes(List<CustomObjectAttribute> customAttributes) {

        this.customAttributes = customAttributes;
    }

    public List<CustomObjectAttribute> getCustomAttributes() {

        return this.customAttributes;
    }

    public boolean hasCustomAttributes() {

        return (this.customAttributes != null && !this.customAttributes.isEmpty());
    }

    public boolean isMultiValuedCustomAttributes() {

        return hasCustomAttributes() && this.customAttributes.size() > 1;
    }

    public CustomObjectAttribute getCustomObjectAttribute(final String name) {

        for(CustomObjectAttribute customAttribute : customAttributes) {
            if(customAttribute.getName().equals(name)) {
                return customAttribute;
            }
        }
        return null;
    }

    public List<String> customAttributeValues(final String name) {


        for(CustomObjectAttribute customAttribute : customAttributes) {
            if(customAttribute.getName().equals(name)) {
                List<Object> values = customAttribute.getValues();
                if(values == null || values.isEmpty()) {
                    return new ArrayList<>();
                }
                return convertToString(values);
            }
        }
        return new ArrayList<>();
    }

    public List<String> customAttributeNames() {

        List<String> ret = new ArrayList<>();
        for(CustomObjectAttribute customAttribute : customAttributes) {
            ret.add(customAttribute.getName());
        }
        return ret;
    }

    public String customAttributeValue(final String attributeName) {

        for(CustomObjectAttribute customAttribute : customAttributes) {
            if(customAttribute.getName().equals(attributeName)) {
                List<Object> values = customAttribute.getValues();
                if(values == null || values.isEmpty()) {
                    return null;
                }
                List<String> ret = convertToString(values);
                if(ret.isEmpty()) {
                    return null;
                }
                return ret.get(0);
            }
        }

        return null;
    }

    private List<String> convertToString(List<Object> values) {

        List<String> ret = new ArrayList<>();
        for(Object val : values) {
            if(val instanceof String strval) {
                ret.add(strval);
            }else {
                ret.add(val.toString());
            }
        }
        return ret;
    }
}