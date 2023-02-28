/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Group
 *
 * @author Yuriy Movchan Date: 11.02.2010
 */
@DataEntry(sortBy = {"displayName"})
@ObjectClass(value = "jansOrganization")
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

    @AttributeName(name = "jansStatus")
    private GluuStatus status;

    @AttributeName(name = "jansManagerGrp")
    private String managerGroup;

    @AttributeName(name = "jansThemeColor")
    private String themeColor;

    @AttributeName(name = "jansOrgShortName")
    private String shortName;

    @AttributeName(name = "jansCustomMessage")
    private String[] customMessages;

    @AttributeName(name = "title")
    private String title;

    @AttributeName(name = "jansLogoPath")
    private String jsLogoPath;

    @AttributeName(name = "jansFaviconPath")
    private String jsFaviconPath;

    public String getJsLogoPath() {
        return jsLogoPath;
    }

    public void setJsLogoPath(String jsLogoPath) {
        this.jsLogoPath = jsLogoPath;
    }

    public String getJsFaviconPath() {
        return jsFaviconPath;
    }

    public void setJsFaviconPath(String jsFaviconPath) {
        this.jsFaviconPath = jsFaviconPath;
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
        if (StringUtils.isBlank(title)) {
            title = "Gluu";
        }
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
