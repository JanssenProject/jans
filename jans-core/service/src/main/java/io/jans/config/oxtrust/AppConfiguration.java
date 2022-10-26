/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.enterprise.inject.Vetoed;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;


/**
 * Janssen Project configuration
 *
 * @author Yuriy Movchan
 * @version 0.1, 05/15/2013
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Configuration, Serializable {

    private static final long serialVersionUID = -8991383390239617013L;

    private String baseDN;

    private String orgSupportEmail;

    private String applicationUrl;

    private String baseEndpoint;

    private String[] personObjectClassTypes;
    private String personCustomObjectClass;

    private String[] personObjectClassDisplayNames;

    private String[] contactObjectClassTypes;
    private String[] contactObjectClassDisplayNames;

    private String ldifStore;

    private boolean updateStatus;

    private String keystorePath;
    private String keystorePassword;

    private boolean allowPersonModification;

    private String idpUrl;

    private String spMetadataPath;

    private String idpSecurityKey;
    private String idpSecurityKeyPassword;
    private String idpSecurityCert;

    private String[] gluuSpAttributes;

    private boolean configGeneration;

    private String idpLdapProtocol;
    private String idpLdapServer;
    private String idpBindDn;
    private String idpBindPassword;
    private String idpUserFields;

    private String gluuSpCert;

    private String shibboleth3FederationRootDir;

    private String caCertsLocation;
    private String caCertsPassphrase;
    private String tempCertDir;
    private String certDir;

    private String servicesRestartTrigger;

    private String oxAuthSectorIdentifierUrl;

    private String oxAuthClientId;
    private String oxAuthClientPassword;
    private String oxAuthClientScope;

    private String loginRedirectUrl;
    private String logoutRedirectUrl;

    private String clientAssociationAttribute;

    private String oxAuthIssuer;

    private boolean ignoreValidation;

    private String umaIssuer;

    private String scimUmaClientId;
    private String scimUmaClientKeyId;
    private String scimUmaResourceId;
    private String scimUmaScope;
    private String scimUmaClientKeyStoreFile;
    private String scimUmaClientKeyStorePassword;

    private String apiUmaClientId;
    private String apiUmaClientKeyId;
    private String apiUmaResourceId;
    private String[] apiUmaScopes;
    private String apiUmaClientKeyStoreFile;
    private String apiUmaClientKeyStorePassword;

    private String cssLocation;
    private String jsLocation;

    private String recaptchaSiteKey;
    private String recaptchaSecretKey;
    private boolean authenticationRecaptchaEnabled;

    private boolean scimTestMode;
    private boolean oxTrustApiTestMode;
    private boolean enableUpdateNotification;

    private boolean rptConnectionPoolUseConnectionPooling;
    private int rptConnectionPoolMaxTotal;
    private int rptConnectionPoolDefaultMaxPerRoute;
    private int rptConnectionPoolValidateAfterInactivity; // In seconds; will be converted to millis
    private int rptConnectionPoolCustomKeepAliveTimeout; // In seconds; will be converted to millis

    private boolean oxIncommonFlag;

    private List<String> clientWhiteList;
    private List<String> clientBlackList;
    private List<String> supportedUserStatus= Arrays.asList("active","inactive");

    private String loggingLevel;
    private String loggingLayout;
    private String externalLoggerConfiguration;

    private String shibbolethVersion;
    private String shibboleth3IdpRootDir;
    private String shibboleth3SpConfDir;
    private String organizationName;
    private String idp3SigningCert;
    private String idp3EncryptionCert;

    private int metricReporterInterval;
    private int metricReporterKeepDataDays;
    private Boolean metricReporterEnabled;
    private Boolean disableJdkLogger = true;

    private int passwordResetRequestExpirationTime; // in seconds
    private int cleanServiceInterval;
    private Boolean enforceEmailUniqueness = true;

    private Boolean useLocalCache = false;

    public boolean isOxIncommonFlag() {
        return oxIncommonFlag;
    }

    public void setOxIncommonFlag(boolean oxIncommonFlag) {
        this.oxIncommonFlag = oxIncommonFlag;
    }

    public String getBaseDN() {
        return baseDN;
    }

    public void setBaseDN(String baseDN) {
        this.baseDN = baseDN;
    }

    public String getOrgSupportEmail() {
        return orgSupportEmail;
    }

    public void setOrgSupportEmail(String orgSupportEmail) {
        this.orgSupportEmail = orgSupportEmail;
    }

    public String getApplicationUrl() {
        return applicationUrl;
    }

    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    public void setBaseEndpoint(String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    public String[] getPersonObjectClassTypes() {
        return personObjectClassTypes;
    }

    public void setPersonObjectClassTypes(String[] personObjectClassTypes) {
        this.personObjectClassTypes = personObjectClassTypes;
    }

    public String getPersonCustomObjectClass() {
        return personCustomObjectClass;
    }

    public void setPersonCustomObjectClass(String personCustomObjectClass) {
        this.personCustomObjectClass = personCustomObjectClass;
    }

    public String[] getPersonObjectClassDisplayNames() {
        return personObjectClassDisplayNames;
    }

    public void setPersonObjectClassDisplayNames(String[] personObjectClassDisplayNames) {
        this.personObjectClassDisplayNames = personObjectClassDisplayNames;
    }

    public String[] getContactObjectClassTypes() {
        return contactObjectClassTypes;
    }

    public void setContactObjectClassTypes(String[] contactObjectClassTypes) {
        this.contactObjectClassTypes = contactObjectClassTypes;
    }

    public String[] getContactObjectClassDisplayNames() {
        return contactObjectClassDisplayNames;
    }

    public void setContactObjectClassDisplayNames(String[] contactObjectClassDisplayNames) {
        this.contactObjectClassDisplayNames = contactObjectClassDisplayNames;
    }



    public String getLdifStore() {
        return ldifStore;
    }

    public void setLdifStore(String ldifStore) {
        this.ldifStore = ldifStore;
    }

    public boolean isUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(boolean updateStatus) {
        this.updateStatus = updateStatus;
    }


    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public boolean isAllowPersonModification() {
        return allowPersonModification;
    }

    public void setAllowPersonModification(boolean allowPersonModification) {
        this.allowPersonModification = allowPersonModification;
    }

    public String getIdpUrl() {
        return idpUrl;
    }

    public void setIdpUrl(String idpUrl) {
        this.idpUrl = idpUrl;
    }

    public String getSpMetadataPath() {
        return spMetadataPath;
    }

    public void setSpMetadataPath(String spMetadataPath) {
        this.spMetadataPath = spMetadataPath;
    }
    public String getIdpSecurityKey() {
        return idpSecurityKey;
    }

    public void setIdpSecurityKey(String idpSecurityKey) {
        this.idpSecurityKey = idpSecurityKey;
    }

    public String getIdpSecurityKeyPassword() {
        return idpSecurityKeyPassword;
    }

    public void setIdpSecurityKeyPassword(String idpSecurityKeyPassword) {
        this.idpSecurityKeyPassword = idpSecurityKeyPassword;
    }

    public String getIdpSecurityCert() {
        return idpSecurityCert;
    }

    public void setIdpSecurityCert(String idpSecurityCert) {
        this.idpSecurityCert = idpSecurityCert;
    }

    public String[] getGluuSpAttributes() {
        return gluuSpAttributes;
    }

    public void setGluuSpAttributes(String[] gluuSpAttributes) {
        this.gluuSpAttributes = gluuSpAttributes;
    }

    public boolean isConfigGeneration() {
        return configGeneration;
    }

    public void setConfigGeneration(boolean configGeneration) {
        this.configGeneration = configGeneration;
    }

    public String getIdpLdapProtocol() {
        return idpLdapProtocol;
    }

    public void setIdpLdapProtocol(String idpLdapProtocol) {
        this.idpLdapProtocol = idpLdapProtocol;
    }

    public String getIdpLdapServer() {
        return idpLdapServer;
    }

    public void setIdpLdapServer(String idpLdapServer) {
        this.idpLdapServer = idpLdapServer;
    }

    public String getIdpBindDn() {
        return idpBindDn;
    }

    public void setIdpBindDn(String idpBindDn) {
        this.idpBindDn = idpBindDn;
    }

    public String getIdpBindPassword() {
        return idpBindPassword;
    }

    public void setIdpBindPassword(String idpBindPassword) {
        this.idpBindPassword = idpBindPassword;
    }

    public String getIdpUserFields() {
        return idpUserFields;
    }

    public void setIdpUserFields(String idpUserFields) {
        this.idpUserFields = idpUserFields;
    }

    public String getGluuSpCert() {
        return gluuSpCert;
    }

    public void setGluuSpCert(String gluuSpCert) {
        this.gluuSpCert = gluuSpCert;
    }

    public String getShibboleth3FederationRootDir() {
        return shibboleth3FederationRootDir;
    }

    public void setShibboleth3FederationRootDir(String shibboleth3FederationRootDir) {
        this.shibboleth3FederationRootDir = shibboleth3FederationRootDir;
    }

    public String getCaCertsLocation() {
        return caCertsLocation;
    }

    public void setCaCertsLocation(String caCertsLocation) {
        this.caCertsLocation = caCertsLocation;
    }

    public String getCaCertsPassphrase() {
        return caCertsPassphrase;
    }

    public void setCaCertsPassphrase(String caCertsPassphrase) {
        this.caCertsPassphrase = caCertsPassphrase;
    }

    public String getTempCertDir() {
        return tempCertDir;
    }

    public void setTempCertDir(String tempCertDir) {
        this.tempCertDir = tempCertDir;
    }

    public String getCertDir() {
        return certDir;
    }

    public void setCertDir(String certDir) {
        this.certDir = certDir;
    }

    public String getServicesRestartTrigger() {
        return servicesRestartTrigger;
    }

    public void setServicesRestartTrigger(String servicesRestartTrigger) {
        this.servicesRestartTrigger = servicesRestartTrigger;
    }

    public String getOxAuthSectorIdentifierUrl() {
        return oxAuthSectorIdentifierUrl;
    }

    public void setOxAuthSectorIdentifierUrl(String oxAuthSectorIdentifierUrl) {
        this.oxAuthSectorIdentifierUrl = oxAuthSectorIdentifierUrl;
    }

    public String getOxAuthClientId() {
        return oxAuthClientId;
    }

    public void setOxAuthClientId(String oxAuthClientId) {
        this.oxAuthClientId = oxAuthClientId;
    }

    public String getOxAuthClientPassword() {
        return oxAuthClientPassword;
    }

    public void setOxAuthClientPassword(String oxAuthClientPassword) {
        this.oxAuthClientPassword = oxAuthClientPassword;
    }

    public String getOxAuthClientScope() {
        return oxAuthClientScope;
    }

    public void setOxAuthClientScope(String oxAuthClientScope) {
        this.oxAuthClientScope = oxAuthClientScope;
    }

    public String getLoginRedirectUrl() {
        return loginRedirectUrl;
    }

    public void setLoginRedirectUrl(String loginRedirectUrl) {
        this.loginRedirectUrl = loginRedirectUrl;
    }

    public String getLogoutRedirectUrl() {
        return logoutRedirectUrl;
    }

    public void setLogoutRedirectUrl(String logoutRedirectUrl) {
        this.logoutRedirectUrl = logoutRedirectUrl;
    }

    public String getClientAssociationAttribute() {
        return clientAssociationAttribute;
    }

    public void setClientAssociationAttribute(String clientAssociationAttribute) {
        this.clientAssociationAttribute = clientAssociationAttribute;
    }

    public String getOxAuthIssuer() {
        return oxAuthIssuer;
    }

    public void setOxAuthIssuer(String oxAuthIssuer) {
        this.oxAuthIssuer = oxAuthIssuer;
    }

    public boolean isIgnoreValidation() {
        return ignoreValidation;
    }

    public void setIgnoreValidation(boolean ignoreValidation) {
        this.ignoreValidation = ignoreValidation;
    }

    public String getUmaIssuer() {
        return umaIssuer;
    }

    public void setUmaIssuer(String umaIssuer) {
        this.umaIssuer = umaIssuer;
    }

    public String getScimUmaClientId() {
        return scimUmaClientId;
    }

    public void setScimUmaClientId(String scimUmaClientId) {
        this.scimUmaClientId = scimUmaClientId;
    }

    public String getScimUmaClientKeyId() {
        return scimUmaClientKeyId;
    }

    public void setScimUmaClientKeyId(String scimUmaClientKeyId) {
        this.scimUmaClientKeyId = scimUmaClientKeyId;
    }

    public String getScimUmaResourceId() {
        return scimUmaResourceId;
    }

    public void setScimUmaResourceId(String scimUmaResourceId) {
        this.scimUmaResourceId = scimUmaResourceId;
    }

    public String getScimUmaScope() {
        return scimUmaScope;
    }

    public void setScimUmaScope(String scimUmaScope) {
        this.scimUmaScope = scimUmaScope;
    }

    public String getScimUmaClientKeyStoreFile() {
        return scimUmaClientKeyStoreFile;
    }

    public void setScimUmaClientKeyStoreFile(String scimUmaClientKeyStoreFile) {
        this.scimUmaClientKeyStoreFile = scimUmaClientKeyStoreFile;
    }

    public String getScimUmaClientKeyStorePassword() {
        return scimUmaClientKeyStorePassword;
    }

    public void setScimUmaClientKeyStorePassword(String scimUmaClientKeyStorePassword) {
        this.scimUmaClientKeyStorePassword = scimUmaClientKeyStorePassword;
    }

    public String getCssLocation() {
        return cssLocation;
    }

    public void setCssLocation(String cssLocation) {
        this.cssLocation = cssLocation;
    }

    public String getJsLocation() {
        return jsLocation;
    }

    public void setJsLocation(String jsLocation) {
        this.jsLocation = jsLocation;
    }

    public String getRecaptchaSiteKey() {
        return recaptchaSiteKey;
    }

    public void setRecaptchaSiteKey(String recaptchaSiteKey) {
        this.recaptchaSiteKey = recaptchaSiteKey;
    }

    public String getRecaptchaSecretKey() {
        return recaptchaSecretKey;
    }

    public void setRecaptchaSecretKey(String recaptchaSecretKey) {
        this.recaptchaSecretKey = recaptchaSecretKey;
    }

    public boolean isScimTestMode() {
        return scimTestMode;
    }

    public void setScimTestMode(boolean scimTestMode) {
        this.scimTestMode = scimTestMode;
    }

    public boolean isOxTrustApiTestMode() {
		return oxTrustApiTestMode;
	}

	public void setOxTrustApiTestMode(boolean oxTrustApiTestMode) {
		this.oxTrustApiTestMode = oxTrustApiTestMode;
	}

	public boolean isRptConnectionPoolUseConnectionPooling() {
        return rptConnectionPoolUseConnectionPooling;
    }

    public void setRptConnectionPoolUseConnectionPooling(boolean rptConnectionPoolUseConnectionPooling) {
        this.rptConnectionPoolUseConnectionPooling = rptConnectionPoolUseConnectionPooling;
    }

    public int getRptConnectionPoolMaxTotal() {
        return rptConnectionPoolMaxTotal;
    }

    public void setRptConnectionPoolMaxTotal(int rptConnectionPoolMaxTotal) {
        this.rptConnectionPoolMaxTotal = rptConnectionPoolMaxTotal;
    }

    public int getRptConnectionPoolDefaultMaxPerRoute() {
        return rptConnectionPoolDefaultMaxPerRoute;
    }

    public void setRptConnectionPoolDefaultMaxPerRoute(int rptConnectionPoolDefaultMaxPerRoute) {
        this.rptConnectionPoolDefaultMaxPerRoute = rptConnectionPoolDefaultMaxPerRoute;
    }

    public int getRptConnectionPoolValidateAfterInactivity() {
        return rptConnectionPoolValidateAfterInactivity;
    }

    public void setRptConnectionPoolValidateAfterInactivity(int rptConnectionPoolValidateAfterInactivity) {
        this.rptConnectionPoolValidateAfterInactivity = rptConnectionPoolValidateAfterInactivity;
    }

    public int getRptConnectionPoolCustomKeepAliveTimeout() {
        return rptConnectionPoolCustomKeepAliveTimeout;
    }

    public void setRptConnectionPoolCustomKeepAliveTimeout(int rptConnectionPoolCustomKeepAliveTimeout) {
        this.rptConnectionPoolCustomKeepAliveTimeout = rptConnectionPoolCustomKeepAliveTimeout;
    }

    public String getShibbolethVersion() {
        return shibbolethVersion;
    }

    public void setShibbolethVersion(String shibbolethVersion) {
        this.shibbolethVersion = shibbolethVersion;
    }

    public String getShibboleth3IdpRootDir() {
        return shibboleth3IdpRootDir;
    }

    public void setShibboleth3IdpRootDir(String shibboleth3IdpRootDir) {
        this.shibboleth3IdpRootDir = shibboleth3IdpRootDir;
    }

    public String getShibboleth3SpConfDir() {
        return shibboleth3SpConfDir;
    }

    public void setShibboleth3SpConfDir(String shibboleth3SpConfDir) {
        this.shibboleth3SpConfDir = shibboleth3SpConfDir;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getIdp3SigningCert() {
        return idp3SigningCert;
    }

    public void setIdp3SigningCert(String idp3SigningCert) {
        this.idp3SigningCert = idp3SigningCert;
    }

    public String getIdp3EncryptionCert() {
        return idp3EncryptionCert;
    }

    public void setIdp3EncryptionCert(String idp3EncryptionCert) {
        this.idp3EncryptionCert = idp3EncryptionCert;
    }

    public List<String> getClientWhiteList() {
        return clientWhiteList;
    }

    public void setClientWhiteList(List<String> clientWhiteList) {
        this.clientWhiteList = clientWhiteList;
    }

    public List<String> getClientBlackList() {
        return clientBlackList;
    }

    public void setClientBlackList(List<String> clientBlackList) {
        this.clientBlackList = clientBlackList;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public String getLoggingLayout() {
        return loggingLayout;
    }

    public void setLoggingLayout(String loggingLayout) {
        this.loggingLayout = loggingLayout;
    }

    public String getExternalLoggerConfiguration() {
		return externalLoggerConfiguration;
	}

	public void setExternalLoggerConfiguration(String externalLoggerConfiguration) {
		this.externalLoggerConfiguration = externalLoggerConfiguration;
	}

	public int getMetricReporterInterval() {
        return metricReporterInterval;
    }

    public void setMetricReporterInterval(int metricReporterInterval) {
        this.metricReporterInterval = metricReporterInterval;
    }

    public List<String> getSupportedUserStatus() {
        return supportedUserStatus;
    }

    public void setSupportedUserStatus(List<String> supportedUserStatus) {
        this.supportedUserStatus = supportedUserStatus;
    }

    /**
     * @return the apiUmaClientId
     */
    public String getApiUmaClientId() {
        return apiUmaClientId;
    }

    /**
     * @param apiUmaClientId
     *            the apiUmaClientId to set
     */
    public void setApiUmaClientId(String apiUmaClientId) {
        this.apiUmaClientId = apiUmaClientId;
    }

    /**
     * @return the apiUmaClientKeyId
     */
    public String getApiUmaClientKeyId() {
        return apiUmaClientKeyId;
    }

    /**
     * @param apiUmaClientKeyId
     *            the apiUmaClientKeyId to set
     */
    public void setApiUmaClientKeyId(String apiUmaClientKeyId) {
        this.apiUmaClientKeyId = apiUmaClientKeyId;
    }

    /**
     * @return the apiUmaResourceId
     */
    public String getApiUmaResourceId() {
        return apiUmaResourceId;
    }

    /**
     * @param apiUmaResourceId
     *            the apiUmaResourceId to set
     */
    public void setApiUmaResourceId(String apiUmaResourceId) {
        this.apiUmaResourceId = apiUmaResourceId;
    }

    public String[] getApiUmaScopes() {
        return apiUmaScopes;
    }

    public void setApiUmaScopes(String[] apiUmaScopes) {
        this.apiUmaScopes = apiUmaScopes;
    }

    /**
     * @return the apiUmaClientKeyStoreFile
     */
    public String getApiUmaClientKeyStoreFile() {
        return apiUmaClientKeyStoreFile;
    }

    /**
     * @param apiUmaClientKeyStoreFile
     *            the apiUmaClientKeyStoreFile to set
     */
    public void setApiUmaClientKeyStoreFile(String apiUmaClientKeyStoreFile) {
        this.apiUmaClientKeyStoreFile = apiUmaClientKeyStoreFile;
    }

    /**
     * @return the apiUmaClientKeyStorePassword
     */
    public String getApiUmaClientKeyStorePassword() {
        return apiUmaClientKeyStorePassword;
    }

    /**
     * @param apiUmaClientKeyStorePassword
     *            the apiUmaClientKeyStorePassword to set
     */
    public void setApiUmaClientKeyStorePassword(String apiUmaClientKeyStorePassword) {
        this.apiUmaClientKeyStorePassword = apiUmaClientKeyStorePassword;
    }

	public boolean isAuthenticationRecaptchaEnabled() {
		return authenticationRecaptchaEnabled;
	}

	public void setAuthenticationRecaptchaEnabled(boolean authenticationRecaptchaEnabled) {
		this.authenticationRecaptchaEnabled = authenticationRecaptchaEnabled;
	}

    public int getMetricReporterKeepDataDays() {
        return metricReporterKeepDataDays;
    }

    public void setMetricReporterKeepDataDays(int metricReporterKeepDataDays) {
        this.metricReporterKeepDataDays = metricReporterKeepDataDays;
    }

    public Boolean getMetricReporterEnabled() {
        return metricReporterEnabled;
    }

    public void setMetricReporterEnabled(Boolean metricReporterEnabled) {
        this.metricReporterEnabled = metricReporterEnabled;
    }

    public Boolean getDisableJdkLogger() {
        return disableJdkLogger;
    }

    public void setDisableJdkLogger(Boolean disableJdkLogger) {
        this.disableJdkLogger = disableJdkLogger;
    }

    public int getPasswordResetRequestExpirationTime() {
        return passwordResetRequestExpirationTime;
    }

    public void setPasswordResetRequestExpirationTime(int passwordResetRequestExpirationTime) {
        this.passwordResetRequestExpirationTime = passwordResetRequestExpirationTime;
    }

    public int getCleanServiceInterval() {
        return cleanServiceInterval;
    }

    public void setCleanServiceInterval(int cleanServiceInterval) {
        this.cleanServiceInterval = cleanServiceInterval;
    }

	public Boolean getEnforceEmailUniqueness() {
		return enforceEmailUniqueness;
	}

	public void setEnforceEmailUniqueness(Boolean enforceEmailUniqueness) {
		this.enforceEmailUniqueness = enforceEmailUniqueness;
	}

	public Boolean getUseLocalCache() {
		return useLocalCache;
	}

	public void setUseLocalCache(Boolean useLocalCache) {
		this.useLocalCache = useLocalCache;
	}

    public boolean isEnableUpdateNotification() {
        return enableUpdateNotification;
    }

    public void setEnableUpdateNotification(boolean enableUpdateNotification) {
        this.enableUpdateNotification = enableUpdateNotification;
    }

}
