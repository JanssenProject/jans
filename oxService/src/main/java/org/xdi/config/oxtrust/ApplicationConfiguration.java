/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.config.oxtrust;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * oxTrust configuration
 * 
 * @author Yuriy Movchan
 * @version 0.1, 05/15/2013
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ApplicationConfiguration implements Serializable {

	private static final long serialVersionUID = -8991383390239617013L;

	private String baseDN;

	private String orgInum;
	private String orgIname;
	private String orgSupportEmail;

	private String applianceInum;
	private String applianceUrl;

	private String baseEndpoint;

	private String schemaAddObjectClassWithoutAttributeTypesDefinition;
	private String schemaAddObjectClassWithAttributeTypesDefinition;

	private String[] personObjectClassTypes;
	private String personCustomObjectClass;

	private String[] personObjectClassDisplayNames;

	private String schemaAddAttributeDefinition;

	private String[] contactObjectClassTypes;
	private String[] contactObjectClassDisplayNames;

	private String photoRepositoryRootDir;
	private int photoRepositoryThumbWidth;
	private int photoRepositoryThumbHeight;
	private int photoRepositoryCountLeveles;
	private int photoRepositoryCountFoldersPerLevel;

	private String authMode;

	private String ldifStore;

	private String shibboleth2IdpRootDir;
	private String shibboleth2SpConfDir;

	private boolean updateApplianceStatus;

	private String svnConfigurationStoreRoot;
	private String svnConfigurationStorePassword;

	private String keystorePath;
	private String keystorePassword;

	private boolean allowPersonModification;

	private String idpUrl;

	private String velocityLog;

	private String spMetadataPath;

	private String logoLocation;

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

	private String mysqlUrl;
	private String mysqlUser;
	private String mysqlPassword;

	private String shibboleth2FederationRootDir;

	private String caCertsLocation;
	private String caCertsPassphrase;
	private String tempCertDir;
	private String certDir;

	private String servicesRestartTrigger;

	private boolean persistSVN;

	private String oxAuthAuthorizeUrl;
	private String oxAuthRegisterUrl;
	private String oxAuthTokenUrl;
	private String oxAuthEndSessionUrl;
	private String oxAuthLogoutUrl;
	private String oxAuthTokenValidationUrl;
	private String oxAuthUserInfo;
    private String oxAuthSectorIdentifierUrl;

	private String oxAuthClientId;
	private String oxAuthClientPassword;
	private String oxAuthClientScope;

	private String loginRedirectUrl;
	private String logoutRedirectUrl;

	private String[] clusteredInums;

	private String clientAssociationAttribute;

	private String oxAuthIssuer;

	private boolean ignoreValidation;

	private String umaIssuer;
	private String umaClientId;
	private String umaClientKeyId;
	private String umaResourceId;
	private String umaScope;
	private String umaClientKeyStoreFile;
	private String umaClientKeyStorePassword;

	private String cssLocation;
	private String jsLocation;
	
	private String recaptchaSiteKey;
	private String recaptchaSecretKey;

	private boolean scimTestMode;
	private String scimTestModeAccessToken;

	private boolean rptConnectionPoolUseConnectionPooling;
	private int rptConnectionPoolMaxTotal;
	private int rptConnectionPoolDefaultMaxPerRoute;
	private int rptConnectionPoolValidateAfterInactivity;  // In seconds; will be converted to millis
	private int rptConnectionPoolCustomKeepAliveTimeout;  // In seconds; will be converted to millis

	public String getBaseDN() {
		return baseDN;
	}

	public void setBaseDN(String baseDN) {
		this.baseDN = baseDN;
	}

	public String getOrgInum() {
		return orgInum;
	}

	public void setOrgInum(String orgInum) {
		this.orgInum = orgInum;
	}

	public String getOrgIname() {
		return orgIname;
	}

	public void setOrgIname(String orgIname) {
		this.orgIname = orgIname;
	}

	public String getOrgSupportEmail() {
		return orgSupportEmail;
	}

	public void setOrgSupportEmail(String orgSupportEmail) {
		this.orgSupportEmail = orgSupportEmail;
	}

	public String getApplianceInum() {
		return applianceInum;
	}

	public void setApplianceInum(String applianceInum) {
		this.applianceInum = applianceInum;
	}

	public String getApplianceUrl() {
		return applianceUrl;
	}

	public void setApplianceUrl(String applianceUrl) {
		this.applianceUrl = applianceUrl;
	}

    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    public void setBaseEndpoint(String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

	public String getSchemaAddObjectClassWithoutAttributeTypesDefinition() {
		return schemaAddObjectClassWithoutAttributeTypesDefinition;
	}

	public void setSchemaAddObjectClassWithoutAttributeTypesDefinition(
			String schemaAddObjectClassWithoutAttributeTypesDefinition) {
		this.schemaAddObjectClassWithoutAttributeTypesDefinition = schemaAddObjectClassWithoutAttributeTypesDefinition;
	}

	public String getSchemaAddObjectClassWithAttributeTypesDefinition() {
		return schemaAddObjectClassWithAttributeTypesDefinition;
	}

	public void setSchemaAddObjectClassWithAttributeTypesDefinition(
			String schemaAddObjectClassWithAttributeTypesDefinition) {
		this.schemaAddObjectClassWithAttributeTypesDefinition = schemaAddObjectClassWithAttributeTypesDefinition;
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

	public void setPersonObjectClassDisplayNames(
			String[] personObjectClassDisplayNames) {
		this.personObjectClassDisplayNames = personObjectClassDisplayNames;
	}

	public String getSchemaAddAttributeDefinition() {
		return schemaAddAttributeDefinition;
	}

	public void setSchemaAddAttributeDefinition(
			String schemaAddAttributeDefinition) {
		this.schemaAddAttributeDefinition = schemaAddAttributeDefinition;
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

	public void setContactObjectClassDisplayNames(
			String[] contactObjectClassDisplayNames) {
		this.contactObjectClassDisplayNames = contactObjectClassDisplayNames;
	}

	public String getPhotoRepositoryRootDir() {
		return photoRepositoryRootDir;
	}

	public void setPhotoRepositoryRootDir(String photoRepositoryRootDir) {
		this.photoRepositoryRootDir = photoRepositoryRootDir;
	}

	public int getPhotoRepositoryThumbWidth() {
		return photoRepositoryThumbWidth;
	}

	public void setPhotoRepositoryThumbWidth(int photoRepositoryThumbWidth) {
		this.photoRepositoryThumbWidth = photoRepositoryThumbWidth;
	}

	public int getPhotoRepositoryThumbHeight() {
		return photoRepositoryThumbHeight;
	}

	public void setPhotoRepositoryThumbHeight(int photoRepositoryThumbHeight) {
		this.photoRepositoryThumbHeight = photoRepositoryThumbHeight;
	}

	public int getPhotoRepositoryCountLeveles() {
		return photoRepositoryCountLeveles;
	}

	public void setPhotoRepositoryCountLeveles(int photoRepositoryCountLeveles) {
		this.photoRepositoryCountLeveles = photoRepositoryCountLeveles;
	}

	public int getPhotoRepositoryCountFoldersPerLevel() {
		return photoRepositoryCountFoldersPerLevel;
	}

	public void setPhotoRepositoryCountFoldersPerLevel(
			int photoRepositoryCountFoldersPerLevel) {
		this.photoRepositoryCountFoldersPerLevel = photoRepositoryCountFoldersPerLevel;
	}

	public String getAuthMode() {
		return authMode;
	}

	public void setAuthMode(String authMode) {
		this.authMode = authMode;
	}

	public String getLdifStore() {
		return ldifStore;
	}

	public void setLdifStore(String ldifStore) {
		this.ldifStore = ldifStore;
	}

	public String getShibboleth2IdpRootDir() {
		return shibboleth2IdpRootDir;
	}

	public void setShibboleth2IdpRootDir(String shibboleth2IdpRootDir) {
		this.shibboleth2IdpRootDir = shibboleth2IdpRootDir;
	}

	public String getShibboleth2SpConfDir() {
		return shibboleth2SpConfDir;
	}

	public void setShibboleth2SpConfDir(String shibboleth2SpConfDir) {
		this.shibboleth2SpConfDir = shibboleth2SpConfDir;
	}

	public boolean isUpdateApplianceStatus() {
		return updateApplianceStatus;
	}

	public void setUpdateApplianceStatus(boolean updateApplianceStatus) {
		this.updateApplianceStatus = updateApplianceStatus;
	}

	public String getSvnConfigurationStoreRoot() {
		return svnConfigurationStoreRoot;
	}

	public void setSvnConfigurationStoreRoot(String svnConfigurationStoreRoot) {
		this.svnConfigurationStoreRoot = svnConfigurationStoreRoot;
	}

	public String getSvnConfigurationStorePassword() {
		return svnConfigurationStorePassword;
	}

	public void setSvnConfigurationStorePassword(
			String svnConfigurationStorePassword) {
		this.svnConfigurationStorePassword = svnConfigurationStorePassword;
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

	public String getVelocityLog() {
		return velocityLog;
	}

	public void setVelocityLog(String velocityLog) {
		this.velocityLog = velocityLog;
	}

	public String getSpMetadataPath() {
		return spMetadataPath;
	}

	public void setSpMetadataPath(String spMetadataPath) {
		this.spMetadataPath = spMetadataPath;
	}

	public String getLogoLocation() {
		return logoLocation;
	}

	public void setLogoLocation(String logoLocation) {
		this.logoLocation = logoLocation;
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

	public String getMysqlUrl() {
		return mysqlUrl;
	}

	public void setMysqlUrl(String mysqlUrl) {
		this.mysqlUrl = mysqlUrl;
	}

	public String getMysqlUser() {
		return mysqlUser;
	}

	public void setMysqlUser(String mysqlUser) {
		this.mysqlUser = mysqlUser;
	}

	public String getMysqlPassword() {
		return mysqlPassword;
	}

	public void setMysqlPassword(String mysqlPassword) {
		this.mysqlPassword = mysqlPassword;
	}

	public String getShibboleth2FederationRootDir() {
		return shibboleth2FederationRootDir;
	}

	public void setShibboleth2FederationRootDir(
			String shibboleth2FederationRootDir) {
		this.shibboleth2FederationRootDir = shibboleth2FederationRootDir;
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

	public boolean isPersistSVN() {
		return persistSVN;
	}

	public void setPersistSVN(boolean persistSVN) {
		this.persistSVN = persistSVN;
	}

	public String getOxAuthAuthorizeUrl() {
		return oxAuthAuthorizeUrl;
	}

	public void setOxAuthAuthorizeUrl(String oxAuthAuthorizeUrl) {
		this.oxAuthAuthorizeUrl = oxAuthAuthorizeUrl;
	}

	public String getOxAuthRegisterUrl() {
		return oxAuthRegisterUrl;
	}

	public void setOxAuthRegisterUrl(String oxAuthRegisterUrl) {
		this.oxAuthRegisterUrl = oxAuthRegisterUrl;
	}

	public String getOxAuthTokenUrl() {
		return oxAuthTokenUrl;
	}

	public void setOxAuthTokenUrl(String oxAuthTokenUrl) {
		this.oxAuthTokenUrl = oxAuthTokenUrl;
	}

	public String getOxAuthEndSessionUrl() {
		return oxAuthEndSessionUrl;
	}

	public void setOxAuthEndSessionUrl(String oxAuthEndSessionUrl) {
		this.oxAuthEndSessionUrl = oxAuthEndSessionUrl;
	}

	public String getOxAuthLogoutUrl() {
		return oxAuthLogoutUrl;
	}

	public void setOxAuthLogoutUrl(String oxAuthLogoutUrl) {
		this.oxAuthLogoutUrl = oxAuthLogoutUrl;
	}

	public String getOxAuthTokenValidationUrl() {
		return oxAuthTokenValidationUrl;
	}

	public void setOxAuthTokenValidationUrl(String oxAuthTokenValidationUrl) {
		this.oxAuthTokenValidationUrl = oxAuthTokenValidationUrl;
	}

	public String getOxAuthUserInfo() {
		return oxAuthUserInfo;
	}

	public void setOxAuthUserInfo(String oxAuthUserInfo) {
		this.oxAuthUserInfo = oxAuthUserInfo;
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

	public String[] getClusteredInums() {
		return clusteredInums;
	}

	public void setClusteredInums(String[] clusteredInums) {
		this.clusteredInums = clusteredInums;
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

	public String getUmaClientId() {
		return umaClientId;
	}

	public void setUmaClientId(String umaClientId) {
		this.umaClientId = umaClientId;
	}

	public String getUmaClientKeyId() {
		return umaClientKeyId;
	}

	public void setUmaClientKeyId(String umaClientKeyId) {
		this.umaClientKeyId = umaClientKeyId;
	}

	public String getUmaResourceId() {
		return umaResourceId;
	}

	public void setUmaResourceId(String umaResourceId) {
		this.umaResourceId = umaResourceId;
	}

	public String getUmaScope() {
		return umaScope;
	}

	public void setUmaScope(String umaScope) {
		this.umaScope = umaScope;
	}

	public String getUmaClientKeyStoreFile() {
		return umaClientKeyStoreFile;
	}

	public void setUmaClientKeyStoreFile(String umaClientKeyStoreFile) {
		this.umaClientKeyStoreFile = umaClientKeyStoreFile;
	}

	public String getUmaClientKeyStorePassword() {
		return umaClientKeyStorePassword;
	}

	public void setUmaClientKeyStorePassword(String umaClientKeyStorePassword) {
		this.umaClientKeyStorePassword = umaClientKeyStorePassword;
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

	public String getScimTestModeAccessToken() {
		return scimTestModeAccessToken;
	}

	public void setScimTestModeAccessToken(String scimTestModeAccessToken) {
		this.scimTestModeAccessToken = scimTestModeAccessToken;
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
}
