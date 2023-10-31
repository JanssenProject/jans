/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.model;


import com.fasterxml.jackson.annotation.JsonInclude;

import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
import io.jans.configapi.core.model.ValidationStatus;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansTrustedIdp")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentityProvider extends Entry implements Serializable {


    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName
    private String owner;

	@NotNull
    @AttributeName(name = "name")
    private String name;

    @NotNull
    @Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
    @AttributeName
    private String displayName;

    @NotNull
    @Size(min = 0, max = 4000, message = "Length of the Description should not exceed 4000")
    @AttributeName
    private String description;
	
    @AttributeName
    private String providerId;

    @AttributeName
    private String internalId;
    
    /**
     * SP metadata file location
     */
    @AttributeName(name = "jansSAMLspMetaDataFN")
    @Hidden
    private String spMetaDataFN;

    @AttributeName(name = "jansSAMLspMetaDataURL")
    private String spMetaDataURL;

    @AttributeName(name = "jansEnabled")
    private boolean enabled;
    
    
    @AttributeName(name = "jansStatus")
    private GluuStatus status;

    @AttributeName(name = "jansValidationStatus")
    private ValidationStatus validationStatus;

    @AttributeName(name = "jansValidationLog")
    private List<String> validationLog;

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getSpMetaDataFN() {
        return spMetaDataFN;
    }

    public void setSpMetaDataFN(String spMetaDataFN) {
        this.spMetaDataFN = spMetaDataFN;
    }

    public String getSpMetaDataURL() {
        return spMetaDataURL;
    }

    public void setSpMetaDataURL(String spMetaDataURL) {
        this.spMetaDataURL = spMetaDataURL;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public GluuStatus getStatus() {
        return status;
    }

    public void setStatus(GluuStatus status) {
        this.status = status;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public List<String> getValidationLog() {
        return validationLog;
    }

    public void setValidationLog(List<String> validationLog) {
        this.validationLog = validationLog;
    }

    @Override
    public String toString() {
        return "IdentityProvider [inum=" + inum + ", owner=" + owner + ", name=" + name + ", displayName=" + displayName
                + ", description=" + description + ", providerId=" + providerId + ", internalId=" + internalId
                + ", spMetaDataFN=" + spMetaDataFN + ", spMetaDataURL=" + spMetaDataURL + ", enabled=" + enabled
                + ", status=" + status + ", validationStatus=" + validationStatus + ", validationLog=" + validationLog
                + "]";
    }    
}
