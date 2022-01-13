/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.model.GluuStatus;
import io.jans.model.SmtpConfiguration;
import io.jans.orm.annotation.*;
import io.jans.orm.model.base.InumEntry;
import io.jans.service.cache.CacheConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 */
@DataEntry
@ObjectClass(value = "jansConf")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GluuConfiguration extends InumEntry implements Serializable {

    @AttributeName
    private String description;

    @AttributeName
    private String displayName;

    @AttributeName(name = "jansHostname", updateOnly = true)
    private String hostname;

    @AttributeName(name = "jansLastUpd", updateOnly = true)
    private Date lastUpdate;

    @AttributeName(name = "gluuConfigurationPollingInterval")
    private String pollingInterval;

    @AttributeName(name = "jansStatus", updateOnly = true)
    private GluuStatus status;

    @AttributeName(name = "userPassword", ignoreDuringRead = true)
    private String userPassword;

    @AttributeName(name = "jansSslExpiry", updateOnly = true)
    private String sslExpiry;

    @AttributeName(name = "jansOrgProfileMgt")
    private boolean profileManagment;

    @AttributeName(name = "jansScimEnabled")
    private boolean scimEnabled;

    @AttributeName(name = "jansEmail")
    private String contactEmail;

    @AttributeName(name = "jansSmtpConf")
    @JsonObject
    private SmtpConfiguration smtpConfiguration;

    @AttributeName(name = "gluuConfigurationDnsServer")
    private String configurationDnsServer;

    @AttributeName(name = "jansenticationMode")
    private String authenticationMode;

    @AttributeName(name = "jansLogConfigLocation")
    private String logConfigLocation;

    @AttributeName(name = "jansCacheConf")
    @JsonObject
    private CacheConfiguration cacheConfiguration;

    @AttributeName(name = "jansDocStoreConf")
    @JsonObject
    private DocumentStoreConfiguration documentStoreConfiguration;

    @CustomObjectClass
    private String[] customObjectClasses;

    public final SmtpConfiguration getSmtpConfiguration() {
        return smtpConfiguration;
    }

    public final void setSmtpConfiguration(SmtpConfiguration smtpConfiguration) {
        this.smtpConfiguration = smtpConfiguration;
    }

    public String getConfigurationDnsServer() {
        return configurationDnsServer;
    }

    public void setConfigurationDnsServer(String configurationDnsServer) {
        this.configurationDnsServer = configurationDnsServer;
    }

    public String getAuthenticationMode() {
        return authenticationMode;
    }

    public void setAuthenticationMode(String authenticationMode) {
        this.authenticationMode = authenticationMode;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getLogConfigLocation() {
        return logConfigLocation;
    }

    public void setLogConfigLocation(String logConfigLocation) {
        this.logConfigLocation = logConfigLocation;
    }

    public String getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(String pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public String getContactEmail() {
        if (this.contactEmail == null || this.contactEmail.isEmpty()) {
            return "example@orgname.com";
        }
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public boolean isProfileManagment() {
        return profileManagment;
    }

    public void setProfileManagment(boolean profileManagment) {
        this.profileManagment = profileManagment;
    }

    public boolean isScimEnabled() {
        return scimEnabled;
    }

    public void setScimEnabled(boolean scimEnabled) {
        this.scimEnabled = scimEnabled;
    }

    public String getSslExpiry() {
        return sslExpiry;
    }

    public void setSslExpiry(String sslExpiry) {
        this.sslExpiry = sslExpiry;
    }

    public GluuStatus getStatus() {
        return status;
    }

    public void setStatus(GluuStatus status) {
        this.status = status;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public CacheConfiguration getCacheConfiguration() {
        return cacheConfiguration;
    }

    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    public DocumentStoreConfiguration getDocumentStoreConfiguration() {
        return documentStoreConfiguration;
    }

    public void setDocumentStoreConfiguration(DocumentStoreConfiguration documentStoreConfiguration) {
        this.documentStoreConfiguration = documentStoreConfiguration;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

}
