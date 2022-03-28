/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.model;

import  io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.util.StringHelper;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


@DataEntry
@ObjectClass(value = "jansPerson")
public class Person extends BasePerson {

    private static final long serialVersionUID = 6634191420188575733L;
    
    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;
    
    @AttributeName(name = "jansAssociatedClnt", consistency = true)
    private List<String> associatedClient;
    
    @AttributeName(name = "c")
    private String countryName;
    
    @AttributeName(name = "displayName")
    private String displayName;
        
    @AttributeName(name = "givenName")
    private String givenName;
    
    @AttributeName(name = "jansManagedOrganizations")
    private List<String> managedOrganizations;
    
    @AttributeName(name = "jansOptOuts")
    private List<String> optOuts;

    @AttributeName(name = "jansStatus")
    private GluuStatus status;
    
    @AttributeName(name = "mail")
    private String mail;
    
    @AttributeName(name = "memberOf")
    private List<String> memberOf;
    
    @AttributeName(name = "o")
    private String organization;
    
    @AttributeName(name = "jansExtUid")
    private List<String> extUid;
    
    @AttributeName(name = "jansOTPCache")
    private List<String> otpCache;
    
    @AttributeName(name = "jansLastLogonTime")
    private Date lastLogonTime;
    
    @AttributeName(name = "jansActive")
    private boolean active;
    
    @AttributeName(name = "jansAddres")
    private List<String> addres;
    
    @AttributeName(name = "jansEmail")
    private List<String> email;
    
    @AttributeName(name = "jansEntitlements")
    private List<String> entitlements;
    
    

    public void setAttribute(String attributeName, String attributeValue, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, attributeValue);
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(attributeName);
        getCustomAttributes().add(attribute);
    }

    @Deprecated
    public void setAttribute(String attributeName, String[] attributeValues) {
        setAttribute(attributeName, attributeValues, null);
    }

    public void setAttribute(String attributeName, String[] attributeValues, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, Arrays.asList(attributeValues));
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(attributeName);
        getCustomAttributes().add(attribute);
    }

    @Deprecated
    public void setAttribute(String attributeName, List<String> attributeValues) {
        setAttribute(attributeName, attributeValues, null);
    }

    public void setAttribute(String attributeName, List<String> attributeValues, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, attributeValues);
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(attributeName);
        getCustomAttributes().add(attribute);
    }

    public void removeAttribute(String attributeName) {
        for (Iterator<CustomObjectAttribute> it = getCustomAttributes().iterator(); it.hasNext(); ) {
            if (StringHelper.equalsIgnoreCase(attributeName, it.next().getName())) {
                it.remove();
                break;
            }
        }
    }

}