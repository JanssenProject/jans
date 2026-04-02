package io.jans.configapi.plugin.shibboleth.util;

public final class Constants {
    
    private Constants() {}
    
    public static final String SHIBBOLETH_READ_ACCESS = "https://jans.io/oauth/config/shibboleth/config.readonly";
    public static final String SHIBBOLETH_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth/config.write";
    public static final String SHIBBOLETH_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth/config.admin";
    
    
    public static final String SHIBBOLETH_TR_READ_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.readonly";
    public static final String SHIBBOLETH_TR_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.write";
    public static final String SHIBBOLETH_TR_DELETE_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.delete";
    public static final String SHIBBOLETH_TR_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.admin";
}
