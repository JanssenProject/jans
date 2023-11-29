/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.kc.idp.broker.model;


import com.fasterxml.jackson.annotation.JsonInclude;

import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
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
@ObjectClass(value = "jansIdp")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JansIdentityProvider extends Entry implements Serializable {


    private transient boolean selected;

	@AttributeName(ignoreDuringUpdate = true)
	private String inum;

	@AttributeName
	private String remoteIdpName;

	@AttributeName
	private String remoteIdpHost;
	
	@AttributeName(name = "selectedSingleSignOnService")
	private String selectedSingleSignOnService;
	
	@AttributeName(name = "supportedSingleSignOnServices")
	private String supportedSingleSignOnServices;
	
	@AttributeName(name = "signingCertificates")
	private String signingCertificates;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getRemoteIdpName() {
        return remoteIdpName;
    }

    public void setRemoteIdpName(String remoteIdpName) {
        this.remoteIdpName = remoteIdpName;
    }

    public String getRemoteIdpHost() {
        return remoteIdpHost;
    }

    public void setRemoteIdpHost(String remoteIdpHost) {
        this.remoteIdpHost = remoteIdpHost;
    }

    public String getSelectedSingleSignOnService() {
        return selectedSingleSignOnService;
    }

    public void setSelectedSingleSignOnService(String selectedSingleSignOnService) {
        this.selectedSingleSignOnService = selectedSingleSignOnService;
    }

    public String getSupportedSingleSignOnServices() {
        return supportedSingleSignOnServices;
    }

    public void setSupportedSingleSignOnServices(String supportedSingleSignOnServices) {
        this.supportedSingleSignOnServices = supportedSingleSignOnServices;
    }

    public String getSigningCertificates() {
        return signingCertificates;
    }

    public void setSigningCertificates(String signingCertificates) {
        this.signingCertificates = signingCertificates;
    }
    
}
