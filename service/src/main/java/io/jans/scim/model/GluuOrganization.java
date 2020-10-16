/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

/**
 * Group
 * 
 * @author Yuriy Movchan Date: 11.02.2010
 */
@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "gluuOrganization")
@JsonInclude(Include.NON_NULL)
public class GluuOrganization extends Entry implements Serializable {

	private static final long serialVersionUID = -8284018077740582699L;

	@NotNull
	@Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
	@AttributeName
	private String displayName;

	@NotNull
	@Size(min = 0, max = 60, message = "Length of the Description should not exceed 60")
	@AttributeName
	private String description;

	@AttributeName(name = "memberOf")
	private String member;

	@AttributeName(name = "c")
	private String countryName;

	@AttributeName(name = "o")
	private String organization;

	@AttributeName
	private String seeAlso;

	@AttributeName(name = "jsStatus")
	private GluuStatus status;

	@AttributeName(name = "jsManagerGroup")
	private String managerGroup;

	@AttributeName(name = "excludeLogoPath")
	private String excludeLogoPath;

	@AttributeName(name = "excludeFaviconPath")
	private String excludeFaviconPath;

	@AttributeName(name = "jsLogoPath")
	private String jsLogoPath;

	@AttributeName(name = "jsFaviconPath")
	private String jsFaviconPath;
	
	@AttributeName(name = "idpLogoPath")
	private String idpLogoPath;

	@AttributeName(name = "idpFaviconPath")
	private String idpFaviconPath;

	@AttributeName(name = "jsThemeColor")
	private String themeColor;

	@AttributeName(name = "jsOrgShortName")
	private String shortName;

	@AttributeName(name = "jsCustomMessage")
	private String[] customMessages;

	@AttributeName(name = "oxInumConfig")
	private String oxInumConfig;

	@AttributeName(name = "title")
	private String title;

	public String getOrganizationTitle() {
		if (title == null || title.trim().equals("")) {
			return "Gluu";
		}
		return title;
	}

	public String getCountryName() {
		return countryName;
	}

	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}

	public String[] getCustomMessages() {
		return customMessages;
	}

	public void setCustomMessages(String[] customMessages) {
		this.customMessages = customMessages;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getManagerGroup() {
		return managerGroup;
	}

	public void setManagerGroup(String managerGroup) {
		this.managerGroup = managerGroup;
	}

	public String getMember() {
		return member;
	}

	public void setMember(String member) {
		this.member = member;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getOxInumConfig() {
		return oxInumConfig;
	}

	public void setOxInumConfig(String oxInumConfig) {
		this.oxInumConfig = oxInumConfig;
	}

	public String getSeeAlso() {
		return seeAlso;
	}

	public void setSeeAlso(String seeAlso) {
		this.seeAlso = seeAlso;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public GluuStatus getStatus() {
		return status;
	}

	public void setStatus(GluuStatus status) {
		this.status = status;
	}

	public String getThemeColor() {
		return themeColor;
	}

	public void setThemeColor(String themeColor) {
		this.themeColor = themeColor;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getOxTrustLogoPath() {
		return excludeLogoPath;
	}

	public void setOxTrustLogoPath(String excludeLogoPath) {
		this.excludeLogoPath = excludeLogoPath;
	}

	public String getOxTrustFaviconPath() {
		return excludeFaviconPath;
	}

	public void setOxTrustFaviconPath(String excludeFaviconPath) {
		this.excludeFaviconPath = excludeFaviconPath;
	}

	public String getOxAuthLogoPath() {
		return jsLogoPath;
	}

	public void setOxAuthLogoPath(String jsLogoPath) {
		this.jsLogoPath = jsLogoPath;
	}

	public String getOxAuthFaviconPath() {
		return jsFaviconPath;
	}

	public void setOxAuthFaviconPath(String jsFaviconPath) {
		this.jsFaviconPath = jsFaviconPath;
	}

	public String getIdpLogoPath() {
		return idpLogoPath;
	}

	public void setIdpLogoPath(String idpLogoPath) {
		this.idpLogoPath = idpLogoPath;
	}

	public String getIdpFaviconPath() {
		return idpFaviconPath;
	}

	public void setIdpFaviconPath(String idpFaviconPath) {
		this.idpFaviconPath = idpFaviconPath;
	}
}
