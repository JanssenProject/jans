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
  
    @AttributeName(name = "userPassword")
    private String userPassword;
    
    @AttributeName(name = "st")
    private String state;
    
    @AttributeName(name = "street")
    private String street;
    
    @AttributeName(name = "l")
    private String city;
    
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
    
    
    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public List<String> getAssociatedClient() {
        return associatedClient;
    }

    public void setAssociatedClient(List<String> associatedClient) {
        this.associatedClient = associatedClient;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public List<String> getManagedOrganizations() {
        return managedOrganizations;
    }

    public void setManagedOrganizations(List<String> managedOrganizations) {
        this.managedOrganizations = managedOrganizations;
    }

    public List<String> getOptOuts() {
        return optOuts;
    }

    public void setOptOuts(List<String> optOuts) {
        this.optOuts = optOuts;
    }

    public GluuStatus getStatus() {
        return status;
    }

    public void setStatus(GluuStatus status) {
        this.status = status;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public List<String> getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(List<String> memberOf) {
        this.memberOf = memberOf;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public List<String> getExternalUid() {
        return externalUid;
    }

    public void setExternalUid(List<String> externalUid) {
        this.externalUid = externalUid;
    }

    public List<String> getOtpCache() {
        return otpCache;
    }

    public void setOtpCache(List<String> otpCache) {
        this.otpCache = otpCache;
    }

    public Date getLastLogonTime() {
        return lastLogonTime;
    }

    public void setLastLogonTime(Date lastLogonTime) {
        this.lastLogonTime = lastLogonTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Address> getAddres() {
        return addres;
    }

    public void setAddres(List<Address> addres) {
        this.addres = addres;
    }

    public List<Email> getEmail() {
        return email;
    }

    public void setEmail(List<Email> email) {
        this.email = email;
    }

    public List<String> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(List<String> entitlements) {
        this.entitlements = entitlements;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public List<String> getImsValue() {
        return imsValue;
    }

    public void setImsValue(List<String> imsValue) {
        this.imsValue = imsValue;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getNameFormatted() {
        return nameFormatted;
    }

    public void setNameFormatted(String nameFormatted) {
        this.nameFormatted = nameFormatted;
    }

    public List<PhoneNumber> getPhoneValue() {
        return phoneValue;
    }

    public void setPhoneValue(List<PhoneNumber> phoneValue) {
        this.phoneValue = phoneValue;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    public String getProfileURL() {
        return profileURL;
    }

    public void setProfileURL(String profileURL) {
        this.profileURL = profileURL;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    public void setHonorificPrefix(String honorificPrefix) {
        this.honorificPrefix = honorificPrefix;
    }

    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    public void setHonorificSuffix(String honorificSuffix) {
        this.honorificSuffix = honorificSuffix;
    }

    public List<X509Certificate> getX509Certificates() {
        return x509Certificates;
    }

    public void setX509Certificates(List<X509Certificate> x509Certificates) {
        this.x509Certificates = x509Certificates;
    }

    public Date getPasswordExpirationDate() {
        return passwordExpirationDate;
    }

    public void setPasswordExpirationDate(Date passwordExpirationDate) {
        this.passwordExpirationDate = passwordExpirationDate;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(Date birthdate) {
        this.birthdate = birthdate;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isPhoneNumberVerified() {
        return phoneNumberVerified;
    }

    public void setPhoneNumberVerified(boolean phoneNumberVerified) {
        this.phoneNumberVerified = phoneNumberVerified;
    }

    public List<Address> getAddress() {
        return address;
    }

    public void setAddress(List<Address> address) {
        this.address = address;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSecretAnswer() {
        return secretAnswer;
    }

    public void setSecretAnswer(String secretAnswer) {
        this.secretAnswer = secretAnswer;
    }

    public String getSecretQuestion() {
        return secretQuestion;
    }

    public void setSecretQuestion(String secretQuestion) {
        this.secretQuestion = secretQuestion;
    }

    public String getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(String seeAlso) {
        this.seeAlso = seeAlso;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getTransientId() {
        return transientId;
    }

    public void setTransientId(String transientId) {
        this.transientId = transientId;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getCountInvalidLogin() {
        return countInvalidLogin;
    }

    public void setCountInvalidLogin(Integer countInvalidLogin) {
        this.countInvalidLogin = countInvalidLogin;
    }

    public String getEnrollmentCode() {
        return enrollmentCode;
    }

    public void setEnrollmentCode(String enrollmentCode) {
        this.enrollmentCode = enrollmentCode;
    }

    public String getImapData() {
        return imapData;
    }

    public void setImapData(String imapData) {
        this.imapData = imapData;
    }

    public List<String> getPpid() {
        return ppid;
    }


    public void setPpid(List<String> ppid) {
        this.ppid = ppid;
    }

    public String getGuid() {
        return guid;
    }


    public void setGuid(String guid) {
        this.guid = guid;
    }


    public String getPreferredMethod() {
        return preferredMethod;
    }


    public void setPreferredMethod(String preferredMethod) {
        this.preferredMethod = preferredMethod;
    }


    public String getUserCertificate() {
        return userCertificate;
    }


    public void setUserCertificate(String userCertificate) {
        this.userCertificate = userCertificate;
    }


    public OTPDevice getOtpDevices() {
        return otpDevices;
    }


    public void setOtpDevices(OTPDevice otpDevices) {
        this.otpDevices = otpDevices;
    }


    public String getMobileDevices() {
        return mobileDevices;
    }


    public void setMobileDevices(String mobileDevices) {
        this.mobileDevices = mobileDevices;
    }


    public String getTrustedDevices() {
        return trustedDevices;
    }


    public void setTrustedDevices(String trustedDevices) {
        this.trustedDevices = trustedDevices;
    }


    public String getStrongAuthPolicy() {
        return strongAuthPolicy;
    }


    public void setStrongAuthPolicy(String strongAuthPolicy) {
        this.strongAuthPolicy = strongAuthPolicy;
    }


    public List<String> getUnlinkedExternalUids() {
        return unlinkedExternalUids;
    }


    public void setUnlinkedExternalUids(List<String> unlinkedExternalUids) {
        this.unlinkedExternalUids = unlinkedExternalUids;
    }


    public String getBackchannelDeviceRegistrationTkn() {
        return backchannelDeviceRegistrationTkn;
    }


    public void setBackchannelDeviceRegistrationTkn(String backchannelDeviceRegistrationTkn) {
        this.backchannelDeviceRegistrationTkn = backchannelDeviceRegistrationTkn;
    }


    public String getBackchannelUsrCode() {
        return backchannelUsrCode;
    }


    public void setBackchannelUsrCode(String backchannelUsrCode) {
        this.backchannelUsrCode = backchannelUsrCode;
    }


    public static long getSerialversionuid() {
        return serialVersionUID;
    }


    public void setAttribute(String attributeName, String attributeValue, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, attributeValue);
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(attributeName);
        getCustomAttributes().add(attribute);
    }

    
    public void setAttribute(String attributeName, String[] attributeValues, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, Arrays.asList(attributeValues));
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(attributeName);
        getCustomAttributes().add(attribute);
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