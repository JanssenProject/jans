package io.jans.configapi.plugin.shibboleth.util;

public final class Constants {

    private Constants() {
    }

    public static final String SHIBBOLETH = "/shibboleth";
    public static final String SHIBBOLETH_PLUGIN_CONFIG = "/shibboleth-plugin-config";
    public static final String TRUST_RELATIONSHIP = "/trust-relationship";
    
    public static final String SP_MODULE = "sp-module";
    public static final String SP_METADATA_FILE_PATTERN = "%s_sp-metadata.xml";

    public static final String INUM = "inum";
    public static final String INUM_PATH_PARAM = "/{inum}";
    public static final String NAME = "name";
    public static final String NAME_PATH_PARAM = "/{name}";    
    public static final String SOURCE = "/source";
    public static final String FILE = "/file";
    public static final String MANUAL = "/manual";
    public static final String URI = "/uri";
    public static final String UPSTREAM = "/upstream";
    public static final String MDQ  = "/mdq";

    public static final String SHIBBOLETH_READ_ACCESS = "https://jans.io/oauth/config/shibboleth.readonly";
    public static final String SHIBBOLETH_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth.write";
    public static final String SHIBBOLETH_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth.admin";

    public static final String SHIBBOLETH_CONFIG_READ_ACCESS = "https://jans.io/oauth/config/shibboleth/config.readonly";
    public static final String SHIBBOLETH_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth/config.write";
    public static final String SHIBBOLETH_CONFIG_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth/config.admin";

    public static final String SHIBBOLETH_TR_READ_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.readonly";
    public static final String SHIBBOLETH_TR_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.write";
    public static final String SHIBBOLETH_TR_DELETE_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.delete";
    public static final String SHIBBOLETH_TR_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.admin";
}
