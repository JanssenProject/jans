/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.model.attribute;

import java.io.Serializable;
import java.util.Arrays;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.gluu.persist.model.base.Entry;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.model.GluuStatus;
import org.xdi.model.scim.ScimCustomAtribute;
import org.xdi.model.user.UserRole;

/**
 * Attribute Metadata
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version February 9, 2015
 */
@LdapEntry(sortBy = { "displayName" })
@LdapObjectClass(values = { "top", "Attribute" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attribute extends Entry implements Serializable {

    private static final long serialVersionUID = 4817004894646725606L;

    @LdapAttribute(ignoreDuringUpdate = true)
    private String inum;

    @LdapAttribute(name = "oxAttributeType")
    private String type;

    @LdapAttribute
    private String lifetime;

    @LdapAttribute(name = "oxSourceAttribute")
    private String sourceAttribute;

    @LdapAttribute
    private String salt;

    @LdapAttribute(name = "oxNameIdType")
    private String nameIdType;

    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Name should contain alphabetical and numeric characters only")
    @Size(min = 1, max = 30, message = "Length of the Name should be between 1 and 30")
    @LdapAttribute(name = "AttributeName")
    private String name;

    @NotNull
    @Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
    @LdapAttribute
    private String displayName;

    @NotNull
    @Size(min = 0, max = 4000, message = "Length of the Description should not exceed 4000")
    @LdapAttribute
    private String description;

    @LdapAttribute(name = "gluuAttributeOrigin")
    private String origin;

    @NotNull
    @LdapAttribute(name = "gluuAttributeType")
    private AttributeDataType dataType;

    @NotNull
    @LdapAttribute(name = "gluuAttributeEditType")
    private UserRole[] editType;

    @NotNull
    @LdapAttribute(name = "gluuAttributeViewType")
    private UserRole[] viewType;

    @LdapAttribute(name = "gluuAttributeUsageType")
    private AttributeUsageType[] usageType;

    @LdapAttribute(name = "oxAuthClaimName")
    private String oxAuthClaimName;

    @LdapAttribute(name = "seeAlso")
    private String seeAlso;

    @LdapAttribute(name = "gluuStatus")
    private GluuStatus status;

    @LdapAttribute(name = "gluuSAML1URI")
    private String saml1Uri;

    @LdapAttribute(name = "gluuSAML2URI")
    private String saml2Uri;

    @LdapAttribute(ignoreDuringUpdate = true)
    private String urn;

    @LdapAttribute(name = "oxSCIMCustomAttribute")
    private ScimCustomAtribute oxSCIMCustomAttribute;

    @LdapAttribute(name = "oxMultivaluedAttribute")
    private Multivalued multivaluedAttribute;

    @LdapJsonObject
    @LdapAttribute(name = "oxValidation")
    private AttributeValidation attributeValidation;

    @LdapAttribute(name = "gluuTooltip")
    private String gluuTooltip;

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLifetime() {
        return lifetime;
    }

    public void setLifetime(String lifetime) {
        this.lifetime = lifetime;
    }

    public String getSourceAttribute() {
        return sourceAttribute;
    }

    public void setSourceAttribute(String sourceAttribute) {
        this.sourceAttribute = sourceAttribute;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getNameIdType() {
        return nameIdType;
    }

    public void setNameIdType(String nameIdType) {
        this.nameIdType = nameIdType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public AttributeDataType getDataType() {
        return dataType;
    }

    public void setDataType(AttributeDataType dataType) {
        this.dataType = dataType;
    }

    public UserRole[] getEditType() {
        return editType;
    }

    public void setEditType(UserRole[] editType) {
        this.editType = editType;
    }

    public UserRole[] getViewType() {
        return viewType;
    }

    public void setViewType(UserRole[] viewType) {
        this.viewType = viewType;
    }

    public AttributeUsageType[] getUsageType() {
        return usageType;
    }

    public void setUsageType(AttributeUsageType[] usageType) {
        this.usageType = usageType;
    }

    public String getOxAuthClaimName() {
        return oxAuthClaimName;
    }

    public void setOxAuthClaimName(String oxAuthClaimName) {
        this.oxAuthClaimName = oxAuthClaimName;
    }

    public String getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(String seeAlso) {
        this.seeAlso = seeAlso;
    }

    public GluuStatus getStatus() {
        return status;
    }

    public void setStatus(GluuStatus status) {
        this.status = status;
    }

    public String getSaml1Uri() {
        return saml1Uri;
    }

    public void setSaml1Uri(String saml1Uri) {
        this.saml1Uri = saml1Uri;
    }

    public String getSaml2Uri() {
        return saml2Uri;
    }

    public void setSaml2Uri(String saml2Uri) {
        this.saml2Uri = saml2Uri;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public ScimCustomAtribute getOxSCIMCustomAttribute() {
        return oxSCIMCustomAttribute;
    }

    public void setOxSCIMCustomAttribute(ScimCustomAtribute oxSCIMCustomAttribute) {
        this.oxSCIMCustomAttribute = oxSCIMCustomAttribute;
    }

    public Multivalued getMultivaluedAttribute() {
        return multivaluedAttribute;
    }

    public void setMultivaluedAttribute(Multivalued multivaluedAttribute) {
        this.multivaluedAttribute = multivaluedAttribute;
    }

    public String getGluuTooltip() {
        return gluuTooltip;
    }

    public void setGluuTooltip(String gluuTooltip) {
        this.gluuTooltip = gluuTooltip;
    }

    public AttributeValidation getAttributeValidation() {
        return attributeValidation;
    }

    public void setAttributeValidation(AttributeValidation attributeValidation) {
        this.attributeValidation = attributeValidation;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + Arrays.hashCode(editType);
        result = prime * result + ((gluuTooltip == null) ? 0 : gluuTooltip.hashCode());
        result = prime * result + ((inum == null) ? 0 : inum.hashCode());
        result = prime * result + ((lifetime == null) ? 0 : lifetime.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nameIdType == null) ? 0 : nameIdType.hashCode());
        result = prime * result + ((origin == null) ? 0 : origin.hashCode());
        result = prime * result + ((oxAuthClaimName == null) ? 0 : oxAuthClaimName.hashCode());
        result = prime * result + ((multivaluedAttribute == null) ? 0 : multivaluedAttribute.hashCode());
        result = prime * result + ((oxSCIMCustomAttribute == null) ? 0 : oxSCIMCustomAttribute.hashCode());
        result = prime * result + ((salt == null) ? 0 : salt.hashCode());
        result = prime * result + ((saml1Uri == null) ? 0 : saml1Uri.hashCode());
        result = prime * result + ((saml2Uri == null) ? 0 : saml2Uri.hashCode());
        result = prime * result + ((seeAlso == null) ? 0 : seeAlso.hashCode());
        result = prime * result + ((sourceAttribute == null) ? 0 : sourceAttribute.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((urn == null) ? 0 : urn.hashCode());
        result = prime * result + Arrays.hashCode(usageType);
        result = prime * result + Arrays.hashCode(viewType);
        return result;
    }

}
