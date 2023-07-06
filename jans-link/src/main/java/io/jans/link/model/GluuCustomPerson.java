/*
 * oxTrust is available under the MIT License (2008). 
 * See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.jans.model.JansCustomAttribute;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * Person
 *
 * @author Yuriy Movchan Date: 10.21.2010
 */
@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansPerson")
@JsonInclude(Include.NON_NULL)
public class GluuCustomPerson extends User
                                    implements Serializable {

    private static final long serialVersionUID = -1879582184398161112L;

    private transient boolean selected;

    private String sourceServerName;
    private String sourceServerUserDn;

    @AttributeName(name = "gluuWhitePagesListed")
    private String gluuAllowPublication;

    @AttributeName(name = "jansGuid")
    private String guid;

    @AttributeName(name = "jansOptOuts")
    private List<String> gluuOptOuts;

    @AttributeName(name = "jansAssociatedClnt")
    private List<String> associatedClient;
    
    @AttributeName(name = "jansPPID")
    private List<String> oxPPID;

   // @JsonObject
    @AttributeName(name = "jansExtUid")
    private List<String> oxExternalUid;
    
    @JsonObject
    @AttributeName(name = "jansOTPDevices")
    private OTPDevice  oxOTPDevices;
    
    @AttributeName(name = "jansMobileDevices")
    private String oxMobileDevices;

    public String getOxMobileDevices() {
		return oxMobileDevices;
	}

	public void setOxMobileDevices(String oxMobileDevices) {
		this.oxMobileDevices = oxMobileDevices;
	}

	public OTPDevice getOxOTPDevices() {
		return oxOTPDevices;
	}

	public void setOxOTPDevices(OTPDevice oxOTPDevices) {
		this.oxOTPDevices = oxOTPDevices;
	}

	@AttributeName(name = "jansCreationTimestamp")
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
        return getAttribute("jansStatus");
    }

    public void setStatus(String value) {
        setAttribute("jansStatus", value);
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
        return geBooleanAttribute("jansSLAManager");
    }

    public void setSLAManager(Boolean value) {
        setBooleanAttribute("jansSLAManager", value);
    }

    public List<String> getMemberOf() {
    	String[] value = {};
        for (JansCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase("memberOf")) {
                value = attribute.getStringValues();
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
        for (JansCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                return idx;
            }
            idx++;
        }

        return idx;
    }

    public String getAttribute(String attributeName) {
        String value = null;
        for (JansCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                value = String.valueOf(attribute.getValue());
                break;
            }
        }
        return value;
    }

    public Boolean geBooleanAttribute(String attributeName) {
        Boolean value = null;
        for (JansCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                value = Boolean.parseBoolean(String.valueOf(attribute.getValue()));
                break;
            }
        }
        return value;
    }
    
    public Object[] getAttributeValues(String attributeName) {
    	Object[] value = null;
        for (JansCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                value = attribute.getValues();
                break;
            }
        }
        return value;
    }

    public Object[] getAttributeArray(String attributeName) {
        JansCustomAttribute gluuCustomAttribute = 
                                getGluuCustomAttribute(attributeName);
        if (gluuCustomAttribute == null) {
            return null;
        } else {
            return gluuCustomAttribute.getValues();
        }
    }

    public JansCustomAttribute getGluuCustomAttribute(String attributeName) {
        for (JansCustomAttribute gluuCustomAttribute : customAttributes) {
            if (gluuCustomAttribute.getName().equalsIgnoreCase(attributeName)) {
                return gluuCustomAttribute;
            }
        }

        return null;
    }

    public void setAttribute(String attributeName, String attributeValue) {
        JansCustomAttribute attribute = new JansCustomAttribute(attributeName, 
                                                                attributeValue);
        customAttributes.remove(attribute);
        customAttributes.add(attribute);
    }

    public void setBooleanAttribute(String attributeName, Boolean attributeValue) {
        JansCustomAttribute attribute = new JansCustomAttribute(attributeName, 
                                                                attributeValue);
        customAttributes.remove(attribute);
        customAttributes.add(attribute);
    }

    public void setAttribute(String attributeName, String[] attributeValue) {
        JansCustomAttribute attribute = new JansCustomAttribute(attributeName, 
                                                                attributeValue);
        customAttributes.remove(attribute);
        customAttributes.add(attribute);
    }

    public void removeAttribute(String attributeName) {
        for (Iterator<JansCustomAttribute> it = customAttributes.iterator(); 
                                                                it.hasNext();) {
            JansCustomAttribute attribute = (JansCustomAttribute) it.next();
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
        this.gluuOptOuts = optOuts;
    }

    public List<String> getGluuOptOuts() {
        return gluuOptOuts;
    }

    public List<String> getAssociatedClient() {
        return this.associatedClient;
    }

    public void setAssociatedClient(List<String> associatedClientDNs) {
        this.associatedClient = associatedClientDNs;
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
        return oxExternalUid;
    }

    public void setOxExternalUid(List<String> oxExternalUid) {
        this.oxExternalUid = oxExternalUid;
    }

    public List<String> getOxPPID() {
        return oxPPID;
    }

    public void setOxPPID(List<String> oxPPID) {
        this.oxPPID = oxPPID;
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
