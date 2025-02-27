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
@ObjectClass(value = "jansPerson")
@JsonInclude(Include.NON_NULL)
public class JansCustomPerson extends User 
                                    implements Serializable {

    private static final long serialVersionUID = -1879582184398161112L;

    private transient boolean selected;

    private String sourceServerName;
    private String sourceServerUserDn;

    @AttributeName(name = "jansGuid")
    private String guid;

    @AttributeName(name = "jansOptOuts")
    private List<String> optOuts;

    @AttributeName(name = "jansAssociatedClnt")
    private List<String> associatedClient;
    
    @AttributeName(name = "jansPPID")
    private List<String> ppid;

   // @JsonObject
    @AttributeName(name = "jansExtUid")
    private List<String> externalUid;
    
    @JsonObject
    @AttributeName(name = "jansOTPDevices")
    private OTPDevice  otpDevices;
    
    @AttributeName(name = "jansMobileDevices")
    private String mobileDevices;

	public String getMobileDevices() {
		return mobileDevices;
	}

	public void setMobileDevices(String mobileDevices) {
		this.mobileDevices = mobileDevices;
	}

	public OTPDevice getOtpDevices() {
		return otpDevices;
	}

	public void setOtpDevices(OTPDevice otpDevices) {
		this.otpDevices = otpDevices;
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
        return Boolean.valueOf(getAttribute("jansSLAManager"));
    }

    public void setSLAManager(Boolean value) {
        setAttribute("jansSLAManager", value.toString());
    }

    public List<String> getMemberOf() {
        String[] value = {};
        for (JansCustomAttribute attribute : customAttributes) {
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
                value = attribute.getValue();
                break;
            }
        }
        return value;
    }
    
    public String[] getAttributeValues(String attributeName) {
        String[] value = null;
        for (JansCustomAttribute attribute : customAttributes) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                value = attribute.getValues();
                break;
            }
        }
        return value;
    }

    public String[] getAttributeArray(String attributeName) {
        JansCustomAttribute gluuCustomAttribute = 
                                getJansCustomAttribute(attributeName);
        if (gluuCustomAttribute == null) {
            return null;
        } else {
            return gluuCustomAttribute.getValues();
        }
    }

    public JansCustomAttribute getJansCustomAttribute(String attributeName) {
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

    public List<String> getOptOuts() {
		return optOuts;
	}

	public void setOptOuts(List<String> optOuts) {
		this.optOuts = optOuts;
	}

	public List<String> getAssociatedClient() {
        return this.associatedClient;
    }

    public void setAssociatedClient(List<String> jsAssociatedClntDNs) {
        this.associatedClient = jsAssociatedClntDNs;
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

	public List<String> getExternalUid() {
		return externalUid;
	}

	public void setExternalUid(List<String> externalUid) {
		this.externalUid = externalUid;
	}

	public List<String> getPpid() {
		return ppid;
	}

	public void setPpid(List<String> ppid) {
		this.ppid = ppid;
	}

	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if(obj != null && getInum()!=null && obj instanceof JansCustomPerson){
            result = getInum().equals(((JansCustomPerson) obj).getInum());
        }

        return result;
    }
    
    public JansCustomPerson clone() throws CloneNotSupportedException{
    	return (JansCustomPerson) super.clone();
    }

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}
}
