/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Person
 *
 * @author Yuriy Movchan Date: 10.21.2010
 */
@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "gluuPerson")
@JsonInclude(Include.NON_NULL)
public class GluuCustomPerson extends User 
                                    implements Serializable {

    private static final long serialVersionUID = -1879582184398161112L;

    private transient boolean selected;

    private String sourceServerName;
    private String sourceServerUserDn;

    @AttributeName(name = "jsWhitePagesListed")
    private String gluuAllowPublication;

    @AttributeName(name = "jsGuid")
    private String guid;

    @AttributeName(name = "jsOptOuts")
    private List<String> jsOptOuts;

    @AttributeName(name = "jsAssociatedClnt")
    private List<String> jsAssociatedClnt;
    
    @AttributeName(name = "jsPPID")
    private List<String> jsPPID;

   // @JsonObject
    @AttributeName(name = "jsExternalUid")
    private List<String> jsExternalUid;
    
    @JsonObject
    @AttributeName(name = "jsOTPDevices")
    private OTPDevice  jsOTPDevices;
    
    @AttributeName(name = "jsMobileDevices")
    private String jsMobileDevices;

    public String getOxMobileDevices() {
		return jsMobileDevices;
	}

	public void setOxMobileDevices(String jsMobileDevices) {
		this.jsMobileDevices = jsMobileDevices;
	}

	public OTPDevice getOxOTPDevices() {
		return jsOTPDevices;
	}

	public void setOxOTPDevices(OTPDevice jsOTPDevices) {
		this.jsOTPDevices = jsOTPDevices;
	}

	@AttributeName(name = "jsCreationTimestamp")
    private Date creationDate;

    @AttributeName
    private Date updatedAt;

    public String getMail() {
        return getAttribute("mail");
    }

    public void setMail(String value) {
        setAttribute("mail", value);
    }
    

    public String getNetworkPoken() {
        return getAttribute("networkPoken");
    }

    public void setNetworkPoken(String value) {
        setAttribute("networkPoken", value);
    }

    public String getCommonName() {
        return getAttribute("cn");
    }

    public void setCommonName(String value) {
        setAttribute("cn", value);
    }

    public String getGivenName() {
        return getAttribute("givenName");
    }

    public void setGivenName(String value) {
        setAttribute("givenName", value);
    }

    public String getStatus() {
        return getAttribute("jsStatus");
    }

    public void setStatus(String value) {
        setAttribute("jsStatus", value);
    }

    public String getUserPassword() {
        return getAttribute("userPassword");
    }

    public void setUserPassword(String value) {
        setAttribute("userPassword", value);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Boolean getSLAManager() {
        return Boolean.valueOf(getAttribute("jsSLAManager"));
    }

    public void setSLAManager(Boolean value) {
        setAttribute("jsSLAManager", value.toString());
    }

    public List<String> getMemberOf() {
        String[] value = {};
        for (GluuCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase("memberOf")) {
                value = attribute.getValues();
                break;
            }
        }
        return Arrays.asList(value);
    }

    public void setMemberOf(List<String> value) {
        setAttribute("memberOf", value.toArray(new String[] {}));
    }

    public String getSurname() {
        return getAttribute("sn");
    }

    public void setSurname(String value) {
        setAttribute("sn", value);
    }

    public void setTimezone(String value) {
        setAttribute("zoneinfo", value);
    }

    public String getTimezone() {
        return getAttribute("zoneinfo");
    }

    public void setPreferredLanguage(String value) {
        setAttribute("preferredLanguage", value);
    }

    public String getPreferredLanguage() {
        return getAttribute("preferredLanguage");
    }

    public int getAttributeIndex(String attributeName) {
        int idx = 0;
        for (GluuCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                return idx;
            }
            idx++;
        }

        return idx;
    }

    public String getAttribute(String attributeName) {
        String value = null;
        for (GluuCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                value = attribute.getValue();
                break;
            }
        }
        return value;
    }
    
    public String[] getAttributeValues(String attributeName) {
        String[] value = null;
        for (GluuCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                value = attribute.getValues();
                break;
            }
        }
        return value;
    }

    public String[] getAttributeArray(String attributeName) {
        GluuCustomAttribute gluuCustomAttribute = 
                                getGluuCustomAttribute(attributeName);
        if (gluuCustomAttribute == null) {
            return null;
        } else {
            return gluuCustomAttribute.getValues();
        }
    }

    public GluuCustomAttribute getGluuCustomAttribute(String attributeName) {
        for (GluuCustomAttribute gluuCustomAttribute : customAttributes) {
            if (gluuCustomAttribute.getName().equalsIgnoreCase(attributeName)) {
                return gluuCustomAttribute;
            }
        }

        return null;
    }

    public void setAttribute(String attributeName, String attributeValue) {
        GluuCustomAttribute attribute = new GluuCustomAttribute(attributeName, 
                                                                attributeValue);
        customAttributes.remove(attribute);
        customAttributes.add(attribute);
    }

    public void setAttribute(String attributeName, String[] attributeValue) {
        GluuCustomAttribute attribute = new GluuCustomAttribute(attributeName, 
                                                                attributeValue);
        customAttributes.remove(attribute);
        customAttributes.add(attribute);
    }

    public void removeAttribute(String attributeName) {
        for (Iterator<GluuCustomAttribute> it = customAttributes.iterator(); 
                                                                it.hasNext();) {
            GluuCustomAttribute attribute = (GluuCustomAttribute) it.next();
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                it.remove();
                break;
            }
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public void setGluuAllowPublication(String allowPublication) {
        this.gluuAllowPublication = allowPublication;
    }

    public String getGluuAllowPublication() {
        return gluuAllowPublication;
    }

    public boolean isAllowPublication() {
        return Boolean.parseBoolean(gluuAllowPublication);
    }

    public void setAllowPublication(boolean allowPublication) {
        this.gluuAllowPublication = Boolean.toString(allowPublication);
    }

    public void setGluuOptOuts(List<String> optOuts) {
        this.jsOptOuts = optOuts;
    }

    public List<String> getGluuOptOuts() {
        return jsOptOuts;
    }

    public List<String> getAssociatedClient() {
        return this.jsAssociatedClnt;
    }

    public void setAssociatedClient(List<String> jsAssociatedClntDNs) {
        this.jsAssociatedClnt = jsAssociatedClntDNs;
    }

    public String getSourceServerName() {
        return sourceServerName;
    }

    public void setSourceServerName(String sourceServerName) {
        this.sourceServerName = sourceServerName;
    }

    public final String getSourceServerUserDn() {
        return sourceServerUserDn;
    }

    public final void setSourceServerUserDn(String sourceServerUserDn) {
        this.sourceServerUserDn = sourceServerUserDn;
    }

    public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

    public List<String> getOxExternalUid() {
        return jsExternalUid;
    }

    public void setOxExternalUid(List<String> jsExternalUid) {
        this.jsExternalUid = jsExternalUid;
    }

    public List<String> getOxPPID() {
        return jsPPID;
    }

    public void setOxPPID(List<String> jsPPID) {
        this.jsPPID = jsPPID;
    }

	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if(obj != null && getInum()!=null && obj instanceof GluuCustomPerson){
            result = getInum().equals(((GluuCustomPerson) obj).getInum());
        }

        return result;
    }
    
    public GluuCustomPerson clone() throws CloneNotSupportedException{
    	return (GluuCustomPerson) super.clone();
    }

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}
}
