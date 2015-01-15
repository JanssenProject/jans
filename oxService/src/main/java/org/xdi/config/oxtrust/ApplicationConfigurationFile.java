/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.config.oxtrust;

import org.xdi.util.properties.FileConfiguration;

/**
 * Mapping from oxTrsut.properties to properties
 * 
 * @author Yuriy Movchan
 * @version 0.1, 05/16/2013
 */
public final class ApplicationConfigurationFile {

	private FileConfiguration applicationConfiguration;

	public ApplicationConfigurationFile(FileConfiguration applicationConfiguration) {
		this.applicationConfiguration = applicationConfiguration;
	}

	public String getBaseDN() {
		return applicationConfiguration.getString("baseDN");
	}

	public String getOrgInum() {
		return applicationConfiguration.getString("orgInum");
	}

	public String getOrgIname() {
		return applicationConfiguration.getString("orgIname");
	}

	public String getLdifStore() {
		return applicationConfiguration.getString("ldifStoreDir");
	}

	public String getApplianceIname() {
		return applicationConfiguration.getString("applianceIname");
	}

	public String getSchemaAddObjectClassWithoutAttributeTypesDefinition() {
		return applicationConfiguration.getString("schema.add-without-attribute-types.objectClass.objectClasses");
	}

	public String getSchemaAddObjectClassWithAttributeTypesDefinition() {
		return applicationConfiguration.getString("schema.add-with-attribute-types.objectClass.objectClasses");
	}

	public String[] getPersonObjectClassTypes() {
		return applicationConfiguration.getStringArray("person-objectClass-types");
	}

	public String getPersonCustomObjectClass() {
		return applicationConfiguration.getString("custom-object-class");
	}

	public String[] getPersonObjectClassDisplayNames() {
		return applicationConfiguration.getStringArray("person-objectClass-displayNames");
	}

	public String getSchemaAddAttributeDefinition() {
		return applicationConfiguration.getString("schema.add.attribute.attributeTypes");
	}

	public String[] getContactObjectClassTypes() {
		return applicationConfiguration.getStringArray("contact-objectClass-types");
	}

	public String[] getContactObjectClassDisplayNames() {
		return applicationConfiguration.getStringArray("contact-objectClass-displayNames");
	}

	public String getApplianceInum() {
		return applicationConfiguration.getString("applianceInum");
	}

	public String getPhotoRepositoryRootDir() {
		return applicationConfiguration.getString("photo.repository.root-dir");
	}

	public int getPhotoRepositoryThumbWidth() {
		return applicationConfiguration.getInt("photo.repository.thumb-width");
	}

	public int getPhotoRepositoryThumbHeight() {
		return applicationConfiguration.getInt("photo.repository.thumb-height");
	}

	public int getPhotoRepositoryCountLeveles() {
		return applicationConfiguration.getInt("photo.repository.count-levels");
	}

	public int getPhotoRepositoryCountFoldersPerLevel() {
		return applicationConfiguration.getInt("photo.repository.count-folders-per-level");
	}

	public String getShibboleth2IdpRootDir() {
		return applicationConfiguration.getString("shibboleth2.idp.root-dir");
	}

	public String getShibboleth2SpConfDir() {
		return applicationConfiguration.getString("shibboleth2.sp.conf-dir");
	}

	public String getPokenApplicationSecret() {
		return applicationConfiguration.getString("poken.application.secret");
	}

	public boolean isUpdateApplianceStatus() {
		return applicationConfiguration.getBoolean("site.update-appliance-status");
	}

	public String getSvnConfigurationStoreRoot() {
		return applicationConfiguration.getString("svn.configuration-store.root");
	}

	public String getSvnConfigurationStorePassword() {
		return applicationConfiguration.getString("svn.configuration-store.password");
	}

	public String getKeystorePath() {
		return applicationConfiguration.getString("keystore.path");
	}

	public String getKeystorePassword() {
		return applicationConfiguration.getString("keystore.password");
	}

	public boolean isAllowPersonModification() {
		return applicationConfiguration.getBoolean("person.allow-modification");
	}

	public String getIdpUrl() {
		return applicationConfiguration.getString("idp.url");
	}

	public String getVelocityLog() {
		return applicationConfiguration.getString("velocity.log");
	}

	public String getSpMetadataPath() {
		return applicationConfiguration.getString("gluuSP.metadata");
	}

	public String getLogoLocation() {
		return applicationConfiguration.getString("logo.location");
	}

	public String getIdpSecurityKey() {
		return applicationConfiguration.getString("idp.securityKey");
	}

	public String getIdpSecurityKeyPassword() {
		return applicationConfiguration.getString("idp.securityKeyPassword");
	}

	public String getIdpSecurityCert() {
		return applicationConfiguration.getString("idp.securityCert");
	}

	public String[] getGluuSpAttributes() {
		return applicationConfiguration.getStringArray("gluuSP.shared.attributes");
	}

	public boolean isConfigGeneration() {
		return "enabled".equalsIgnoreCase(applicationConfiguration.getString("configGeneration"));
	}

	public String getIdpLdapProtocol() {
		return "true".equalsIgnoreCase(applicationConfiguration.getString("idp.useSSL")) ? "ldaps" : "ldap";
	}

	public String getIdpLdapServer() {
		return applicationConfiguration.getString("idp.ldap.server");
	}

	public String getIdpBindDn() {
		return applicationConfiguration.getString("idp.bindDN");
	}

	public String getIdpBindPassword() {
		return applicationConfiguration.getString("idp.bindPassword");
	}

	public String getIdpUserFields() {
		return applicationConfiguration.getString("idp.user.fields");
	}

	public String getGluuSpCert() {
		return applicationConfiguration.getString("gluuSP.securityCert");
	}

	public String getApplianceUrl() {
		return applicationConfiguration.getString("appliance.url");
	}

	public String getMysqlUrl() {
		return applicationConfiguration.getString("mysql.url");
	}

	public String getMysqlUser() {
		return applicationConfiguration.getString("mysql.user");
	}

	public String getMysqlPassword() {
		return applicationConfiguration.getString("mysql.password");
	}

	public String getShibboleth2FederationRootDir() {
		return applicationConfiguration.getString("shibboleth2.federation.root-dir");
	}

	public boolean isCacheRefreshEnabled() {
		return applicationConfiguration.getBoolean("cache.refresh.enabled", false);
	}

	public int getCacheRefreshIntervalMinutes() {
		return applicationConfiguration.getInt("cache.refresh.interval.minutes", -1);
	}

	public String getCaCertsLocation() {
		return applicationConfiguration.getString("cacertsLocation");
	}

	public String getCaCertsPassphrase() {
		return applicationConfiguration.getString("cacertsPassphrase");
	}

	public String getTempCertDir() {
		return applicationConfiguration.getString("certDirTemp");
	}

	public String getCertDir() {
		return applicationConfiguration.getString("certDir");
	}

	public String getServicesRestartTrigger() {
		return applicationConfiguration.getString("servicesRestartTrigger");
	}

	public boolean isPersistSVN() {
		return applicationConfiguration.getBoolean("persist-in-svn");
	}

	public String getOxAuthAuthorizeUrl() {
		return applicationConfiguration.getString("oxauth.authorize.url");
	}

	public String getOxAuthTokenUrl() {
		return applicationConfiguration.getString("oxauth.token.url");
	}

	public String getOxAuthValidateTokenUrl() {
		return applicationConfiguration.getString("oxauth.token.validation.url");
	}

	public String getOxAuthLogoutUrl() {
		return applicationConfiguration.getString("oxauth.logout.url");
	}

	public String getOxAuthEndSessionUrl() {
		return applicationConfiguration.getString("oxauth.end.session.url");
	}

	public String getLoginRedirectUrl() {
		return applicationConfiguration.getString("login.redirect.url");
	}

	public String getLogoutRedirectUrl() {
		return applicationConfiguration.getString("logout.redirect.url");
	}

	public String getOxAuthClientId() {
		return applicationConfiguration.getString("oxauth.client.id");
	}

	public String getOxAuthClientPassword() {
		return applicationConfiguration.getString("oxauth.client.password");
	}

	public String getOxAuthClientScope() {
		return applicationConfiguration.getString("oxauth.client.scope");
	}

	public String getOrgDisplayName() {
		return applicationConfiguration.getString("orgDisplayName");
	}

	public String getOrgShortName() {
		return applicationConfiguration.getString("orgShortName");
	}

	public String getOrgSupportEmail() {
		return applicationConfiguration.getString("orgSupportEmail");
	}

	public String getOxPlusIname() {
		return applicationConfiguration.getString("oxplusIname");
	}

	public String[] getClusteredInums() {
		return applicationConfiguration.getStringArray("clusteredInums");
	}

	public String getOxPlussClassXri() {
		return applicationConfiguration.getString("oxPlus.class.xri");
	}

	public String getAuthMode() {
		return applicationConfiguration.getString("oxtrust.auth.mode");
	}

	public String getOxAuthTokenValidationUrl() {
		return applicationConfiguration.getString("oxauth.token.validation.url");
	}

	public String getOxAuthUserInfo() {
		return applicationConfiguration.getString("oxauth.userinfo.url");
	}

	public String getClientAssociationAttribute() {
		return applicationConfiguration.getString("client.association.attribute");
	}

	public boolean isIgnoreValidation() {
		return applicationConfiguration.getBoolean("ignoreValidation", false);
	}

	public String getOxAuthIssuer() {
		return applicationConfiguration.getString("oxauth.issuer");
	}

	public String getUmaIssuer() {
		return applicationConfiguration.getString("uma.issuer");
	}

	public String getUmaClientId() {
		return applicationConfiguration.getString("uma.client_id");
	}

	public String getUmaClientPassword() {
		return applicationConfiguration.getString("uma.client_password");
	}

	public String getUmaResourceId() {
		return applicationConfiguration.getString("uma.resource_id");
	}

	public String getUmaScope() {
		return applicationConfiguration.getString("uma.scope");
	}
	
	public String getCssLocation() {
		return applicationConfiguration.getString("cssLocation");
	}
	
	public String getJsLocation() {
		return applicationConfiguration.getString("jsLocation");
	}
	
	public String getEncodeSalt(){
		return applicationConfiguration.getString("encode_salt");
	}
}
