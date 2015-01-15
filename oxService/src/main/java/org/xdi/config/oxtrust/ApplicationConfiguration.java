/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.config.oxtrust;

import java.io.Serializable;

import lombok.Data;

/**
 * oxTrust configuration
 * 
 * @author Yuriy Movchan
 * @version 0.1, 05/15/2013
 */
public final @Data class ApplicationConfiguration implements Serializable {
	
	private static final long serialVersionUID = -8991383390239617013L;

	private String baseDN;

	private String orgInum;
	private String orgIname;
	private String orgDisplayName;
	private String orgShortName;
	private String orgSupportEmail;

	private String applianceIname;
	private String applianceInum;
	private String applianceUrl;

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

	private String pokenApplicationSecret;

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

	private boolean cacheRefreshEnabled;

	private int cacheRefreshIntervalMinutes;

	private String caCertsLocation;
	private String caCertsPassphrase;
	private String tempCertDir;
	private String certDir;

	private String servicesRestartTrigger;

	private boolean persistSVN;

	private String oxAuthAuthorizeUrl;
	private String oxAuthTokenUrl;
	private String oxAuthValidateTokenUrl;
	private String oxAuthEndSessionUrl;
	private String oxAuthLogoutUrl;

	private String oxAuthTokenValidationUrl;
	private String oxAuthUserInfo;

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
	private String umaClientPassword;
	private String umaResourceId;
	private String umaScope;

	private String cssLocation;
	private String jsLocation;

}
