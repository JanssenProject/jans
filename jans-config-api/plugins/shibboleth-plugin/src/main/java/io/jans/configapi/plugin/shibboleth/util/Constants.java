package io.jans.configapi.plugin.shibboleth.util;

public final class Constants {
    
    private Constants() {}
    
    public static final String SHIBBOLETH_CONFIG = "/shibboleth-config";
    public static final String TRUST_RELATIONSHIP = "/trust-relationship";
    public static final String TRUST_RELATIONSHIP_METADATA_FILE = "/file";
    
    
    public static final String SP_MODULE = "sp-module";
    public static final String SP_METADATA_FILE_PATTERN = "%s_sp-metadata.xml";
    
    public static final String SHIBBOLETH_READ_ACCESS = "https://jans.io/oauth/config/shibboleth.readonly";
    public static final String SHIBBOLETH_WRITE_ACCESS = "https://jans.io/oauth/config/config.write";
    public static final String SHIBBOLETH_ADMIN_ACCESS = "https://jans.io/oauth/config/config.admin";
    
    public static final String SHIBBOLETH_CONFIG_READ_ACCESS = "https://jans.io/oauth/config/shibboleth/config.readonly";
    public static final String SHIBBOLETH_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth/config.write";
    public static final String SHIBBOLETH_CONFIG_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth/config.admin";
    
    
    public static final String SHIBBOLETH_TR_READ_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.readonly";
    public static final String SHIBBOLETH_TR_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.write";
    public static final String SHIBBOLETH_TR_DELETE_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.delete";
    public static final String SHIBBOLETH_TR_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.admin";
}
