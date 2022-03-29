/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package  io.jans.configapi.rest.model.user;

import io.jans.configapi.model.user.Address;
import io.jans.configapi.model.user.Email;
import io.jans.configapi.model.user.Entitlement;
import io.jans.configapi.model.user.InstantMessagingAddress;
import io.jans.configapi.model.user.OTPDevice;
import io.jans.configapi.model.user.PhoneNumber;
import io.jans.configapi.model.user.Photo;
import io.jans.configapi.model.user.Role;
import io.jans.configapi.model.user.X509Certificate;
import io.jans.model.GluuStatus;
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@DataEntry
@ObjectClass(value = "jansPerson")
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private List<String> externalUid;
    
    @AttributeName(name = "jansOTPCache")
    private List<String> otpCache;
    
    @AttributeName(name = "jansLastLogonTime")
    private Date lastLogonTime;
    
    @AttributeName(name = "jansActive")
    private boolean active;
    
    @AttributeName(name = "jansAddres")
    private List<Address> addres;
    
    @AttributeName(name = "jansEmail")
    private List<Email> email;
    
    @AttributeName(name = "jansEntitlements")
    private List<String> entitlements;
    
    @AttributeName(name = "jansExtId")
    private String extId;
    
    @AttributeName(name = "jansImsValue")
    private List<String> imsValue;
    
    @AttributeName(name = "jansLastLogonTime")
    private Date created;
    
    @AttributeName(name = "jansMetaLastMod")
    private Date lastModified;
    
    @AttributeName(name = "jansMetaLocation")
    private String location;
    
    @AttributeName(name = "jansMetaVer")
    private String version;
    
    @AttributeName(name = "jansNameFormatted")
    private String nameFormatted;
    
    @AttributeName(name = "jansPhoneValue")
    private List<PhoneNumber> phoneValue;
    
    @AttributeName(name = "jansPhotos")
    private List<Photo> photos;
    
    @AttributeName(name = "jansProfileURL")
    private String profileURL;
    
    @AttributeName(name = "jansRole")
    private List<Role> roles;
    
    @AttributeName(name = "jansTitle")
    private String title;
    
    @AttributeName(name = "jansUsrTyp")
    private String userType;
    
    @AttributeName(name = "jansHonorificPrefix")
    private String honorificPrefix;
    
    @AttributeName(name = "jansHonorificSuffix")
    private String honorificSuffix;
    
    @AttributeName(name = "jans509Certificate")
    private List<X509Certificate> x509Certificates;
    
    @AttributeName(name = "jansPassExpDate")
    private Date passwordExpirationDate;
    
    @AttributeName(name = "persistentId")
    private String persistentId;
    
    @AttributeName(name = "middleName")
    private String middleName;
    
    @AttributeName(name = "nickname")
    private String nickname;
    
    @AttributeName(name = "jansPrefUsrName")
    private String preferredUsername;

    @AttributeName(name = "profile")
    private String profile;    
    
    @AttributeName(name = "picture")
    private String picture;    
    
    @AttributeName(name = "website")
    private String website;
    
    @AttributeName(name = "emailVerified")
    private boolean emailVerified;
    
    @AttributeName(name = "gender")
    private String gender;
    
    @AttributeName(name = "birthdate")
    private Date birthdate;
    
    @AttributeName(name = "zoneinfo")
    private String timezone;
    
    @AttributeName(name = "locale")
    private String locale;
    
    @AttributeName(name = "phoneNumberVerified")
    private boolean phoneNumberVerified;
    
    @AttributeName(name = "address")
    private List<Address> address;
    
    @AttributeName(name = "updatedAt")
    private Date updatedAt;
    
    @AttributeName(name = "preferredLanguage")
    private String preferredLanguage;    
    
    @AttributeName(name = "role")
    private String role;
    
    @AttributeName(name = "secretAnswer")
    private String secretAnswer;
    
    @AttributeName(name = "secretQuestion")
    private String secretQuestion;
    
    @AttributeName(name = "seeAlso")
    private String seeAlso;
    
    @AttributeName(name = "sn")
    private String sn;
    
    @AttributeName(name = "cn")
    private String cn;
    
    @AttributeName(name = "transientId")
    private String transientId;    
    
    @AttributeName(name = "uid")
    private String uid;    
    
    @AttributeName(name = "userPassword")
    private String userPassword;
    
    @AttributeName(name = "st")
    private String st;
    
    @AttributeName(name = "street")
    private String street;
    
    @AttributeName(name = "l")
    private String l;
    
    @AttributeName(name = "jansCountInvalidLogin")
    private Integer countInvalidLogin;
    
    @AttributeName(name = "jansEnrollmentCode")
    private String enrollmentCode;
    
    @AttributeName(name = "jansIMAPData")
    private String imapData;
    
    @AttributeName(name = "jansPPID")
    private List<String> ppid;
    
    @AttributeName(name = "jansGuid")
    private String guid;
    
    @AttributeName(name = "jansPreferredMethod")
    private String preferredMethod;
    
    @AttributeName(name = "userCertificate")
    private String userCertificate;
    
    @AttributeName(name = "jansOTPDevices")
    private OTPDevice  otpDevices;
    
    @AttributeName(name = "jansMobileDevices")
    private String mobileDevices;
    
    @AttributeName(name = "jansTrustedDevices")
    private String trustedDevices;
    
    @AttributeName(name = "jansStrongAuthPolicy")
    private String strongAuthPolicy;
    
    @AttributeName(name = "jansUnlinkedExternalUids")
    private List<String> unlinkedExternalUids;
    
    @AttributeName(name = "jansBackchannelDeviceRegistrationTkn")
    private String backchannelDeviceRegistrationTkn;
    
    @AttributeName(name = "jansBackchannelUsrCode")
    private String backchannelUsrCode;
    
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