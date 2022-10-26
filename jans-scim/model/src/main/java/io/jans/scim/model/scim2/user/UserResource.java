/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.user;

import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.Validations;
import io.jans.scim.model.scim2.annotations.*;

import java.util.List;

/**
 * An instance of this class represents a User resource as explained in section 4.1 of RFC 7643.
 */
/*
 * Created by jgomer on 2017-09-04.
 *
 * Notes: Property names (member names) MUST match exactly as in the spec, so do not change!. Other classes may depend on
 * this one via reflection. Do not add members whose names are already at io.jans.scim.model.scim2.BaseScimResource.
 * Annotations applied at every member resemble what the spec states
 */
@Schema(id="urn:ietf:params:scim:schemas:core:2.0:User", name="User", description = "User Account")
public class UserResource extends BaseScimResource {

    @Attribute(description = "Unique identifier for the User typically used by the user to directly authenticate to the " +
            "service provider.",
            isRequired = true,
            uniqueness = AttributeDefinition.Uniqueness.SERVER)
    @StoreReference(ref = "uid")
    private String userName;

    @Attribute(description = "The components of the user's real name.",
            type = AttributeDefinition.Type.COMPLEX)
    private Name name;

    @Attribute(description = "The name of the User, suitable for display to end-users. The name SHOULD be the full name " +
            "of the User being described if known.")
    @StoreReference(ref = "displayName")
    private String displayName;

    @Attribute(description = "The casual way to address the user in real life, e.g.'Bob' or 'Bobby' instead of 'Robert'." +
            "This attribute SHOULD NOT be used to represent a User's username (e.g., bjensen or mpepperidge)")
    @StoreReference(ref = "nickname")
    private String nickName;

    @Attribute(description = "A fully qualified URL to a page representing the User's online profile",
            referenceTypes = { "external" },
            type = AttributeDefinition.Type.REFERENCE)
    @StoreReference(ref = "jansProfileURL")
    private String profileUrl;

    @Attribute(description = "The user's title, such as 'Vice President'.")
    @StoreReference(ref = "jansTitle")
    private String title;

    @Attribute(description = "Used to identify the organization to user relationship. Typical values used might be " +
            "'Contractor', 'Employee', 'Intern', 'Temp', 'External', and 'Unknown' but any value may be used.")
    @StoreReference(ref = "jansUsrTyp")
    private String userType;

    @Attribute(description = "Indicates the User's preferred written or spoken language.  Generally used for selecting a " +
            "localized User interface. e.g., 'en_US' specifies the language English and country US.")
    @StoreReference(ref = "preferredLanguage")
    private String preferredLanguage;

    @Attribute(description = "Used to indicate the User's default  location for purposes of localizing items such as " +
            "currency, date time format, numerical representations, etc.")
    @StoreReference(ref = "locale")
    @Validator(value = Validations.LOCALE)
    private String locale;

    @Attribute(description = "The User's time zone in the 'Olson' timezone database format; e.g.,'America/Los_Angeles'")
    @StoreReference(ref = "zoneinfo")
    @Validator(value = Validations.TIMEZONE)
    private String timezone;

    @Attribute(description = "A Boolean value indicating the User's administrative status.",
            type = AttributeDefinition.Type.BOOLEAN)
    @StoreReference(ref = "jansActive")
    private Boolean active;

    @Attribute(description = "The User's clear text password. This attribute is intended to be used as a means to specify" +
            " an initial password when creating a new User or to reset an existing User's password.",
            mutability = AttributeDefinition.Mutability.WRITE_ONLY,
            returned = AttributeDefinition.Returned.NEVER)
    //@StoreReference(ref = "password")     we'd better not annotate this field (prevents end-users issuing password-based searches)
    private String password;

    @Attribute(description = "E-mail addresses for the user. The value SHOULD be canonicalized by the Service Provider, " +
            "e.g., bjensen@example.com instead of bjensen@EXAMPLE.COM. Canonical Type values of work, home, and other.",
            multiValueClass = Email.class,
            type = AttributeDefinition.Type.COMPLEX)
    @StoreReference(ref = "jansEmail")
    private List<Email> emails;

    @Attribute(description = "Phone numbers for the User.  The value SHOULD be canonicalized by the Service Provider " +
            "according to format in RFC3966 e.g., 'tel:+1-201-555-0123'.  Canonical Type values of work, home, mobile, " +
            "fax, pager and other.",
            multiValueClass = PhoneNumber.class,
            type = AttributeDefinition.Type.COMPLEX)
    @StoreReference(ref = "jansPhoneValue")
    private List<PhoneNumber> phoneNumbers;

    @Attribute(description = "Instant messaging addresses for the User.",
            multiValueClass = InstantMessagingAddress.class,
            type = AttributeDefinition.Type.COMPLEX)
    @StoreReference(ref = "jansImsValue")
    private List<InstantMessagingAddress> ims;

    @Attribute(description = "URIs of photos of the User.",
            multiValueClass = Photo.class,
            type = AttributeDefinition.Type.COMPLEX)
    @StoreReference(ref = "jansPhotos")
    private List<Photo> photos;

    @Attribute(description = "Physical mailing addresses for this User.",
            multiValueClass = Address.class,
            type = AttributeDefinition.Type.COMPLEX)
    @StoreReference(ref = "jansAddres")
    private List<Address> addresses;

    @Attribute(description = "A list of groups that the user belongs to, either thorough direct membership, nested groups, " +
            "or dynamically calculated.",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            multiValueClass = Group.class,
            type = AttributeDefinition.Type.COMPLEX)
    private List<Group> groups;

    @Attribute(description = "A list of entitlements for the User that represent a thing the User has.",
            multiValueClass = Entitlement.class,
            type = AttributeDefinition.Type.COMPLEX)
    @StoreReference(ref = "jansEntitlements")
    private List<Entitlement> entitlements;

    @Attribute(description = "A list of roles for the User that collectively represent who the User is; e.g., 'Student'," +
            " 'Faculty'.",
            multiValueClass = Role.class,
            type = AttributeDefinition.Type.COMPLEX)
    @StoreReference(ref = "jansRole")
    private List<Role> roles;

    @Attribute(description = "A list of certificates issued to the User.",
            multiValueClass = X509Certificate.class,
            type = AttributeDefinition.Type.COMPLEX)
    @StoreReference(ref = "jans509Certificate")
    private List<X509Certificate> x509Certificates;

    //This attribute is not present in SCIM spec... but see https://github.com/GluuFederation/SCIM-Client/issues/19
    @Attribute(description = "Pairwise IDs",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            returned = AttributeDefinition.Returned.REQUEST,
            multiValueClass = String.class)
    @StoreReference(ref = "jansPPID")
    private List<String> pairwiseIdentifiers;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
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

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(List<Email> emails) {
        this.emails = emails;
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public List<InstantMessagingAddress> getIms() {
        return ims;
    }

    public void setIms(List<InstantMessagingAddress> ims) {
        this.ims = ims;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public List<Entitlement> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(List<Entitlement> entitlements) {
        this.entitlements = entitlements;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public List<X509Certificate> getX509Certificates() {
        return x509Certificates;
    }

    public void setX509Certificates(List<X509Certificate> x509Certificates) {
        this.x509Certificates = x509Certificates;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<String> getPairwiseIdentifiers() {
        return pairwiseIdentifiers;
    }

    public void setPairwiseIdentifiers(List<String> pairwiseIdentifiers) {
        this.pairwiseIdentifiers = pairwiseIdentifiers;
    }

}
