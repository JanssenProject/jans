package io.jans.kc.protocol.mapper.model;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

import io.jans.model.JansAttribute;
import io.jans.orm.annotation.*;
import io.jans.orm.model.base.CustomObjectAttribute;

@DataEntry
@ObjectClass(value="jansPerson")
public class JansPerson implements Serializable {

    private static final long serialVersionUID = 1L;

    @DN
    private String dn;

    @AttributesList(name="name",value="values",multiValued="multiValued")
    private List<CustomObjectAttribute> customAttributes = new ArrayList<>();


    public JansPerson() {

    }

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

    public List<String> customAttributeValues(final JansAttribute attributeMeta) {


        for(CustomObjectAttribute customAttribute : customAttributes) {
            if(customAttribute.getName().equals(attributeMeta.getName())) {
                List<Object> values = customAttribute.getValues();
                if(values == null || values.size() == 0) {
                    return null;
                }
                return convertToString(values,attributeMeta);
            }
        }
        return null;
    }

    private List<String> convertToString(List<Object> values, final JansAttribute attributeMeta) {

        List<String> ret = new ArrayList<>();
        for(Object val : values) {
            if(val instanceof String) {
                ret.add((String) val);
            }else {
                ret.add(val.toString());
            }
        }
        return ret;
    }
}