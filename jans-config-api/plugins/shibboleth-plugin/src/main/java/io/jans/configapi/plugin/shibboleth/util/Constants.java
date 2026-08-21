package io.jans.configapi.plugin.shibboleth.util;

public final class Constants {

    private Constants() {
    }

    public static final String SHIBBOLETH = "/shibboleth";
    public static final String SHIBBOLETH_PLUGIN_CONFIG = "/shibboleth-plugin-config";
    public static final String TRUST_RELATIONSHIP_PATH = "/trust-relationships";
    
    public static final String SP_MODULE = "sp-module";
    public static final String SP_METADATA_FILE_PATTERN = "%s_sp-metadata.xml";

	public static final String ID = "id";
    public static final String ID_PATH = "/id";
    public static final String ID_PATH_PARAM = "/{id}";
	
    public static final String INUM = "inum";
	public static final String INUM_PATH = "/inum";
    public static final String INUM_PATH_PARAM = "/{inum}";
	
    public static final String NAME = "name";
	public static final String NAME_PATH = "/name";
    public static final String NAME_PATH_PARAM = "/{name}";   
	
	public static final String METADATA_SOURCE_PATH = "/metadata-source";
	
	public static final String PROFILES = "profiles";
    public static final String PROFILES_PATH = "/profiles";	
	public static final String PROFILE_PATH_PARAM = "/{profile}";
	
	
	public static final String RELEASED_ATTRIBUTES_PATH = "/released-attributes";
	public static final String ACTIONS_PATH = "/actions";
    public static final String ACTIVATE_PATH = "/activate";
    public static final String CANCEL_ACTIVATION_PATH = "/cancel-activation";	
	public static final String DEACTIVATE_PATH = "/deactivate";
		
    public static final String SAML_PROFILES = "saml-profiles";
    public static final String RELEASE_POLICY = "/release-policy";
    public static final String EFFECTIVE = "/effective";  
    
    public static final String SOURCE = "/source";
    public static final String FILE = "/file";
    public static final String MANUAL = "/manual";
    public static final String URI = "/uri";
    public static final String UPSTREAM = "/upstream";
    public static final String MDQ  = "/mdq";
    public static final String DISCOVERY = "/discovery";
    public static final String ENTITIES = "/entities";
    
    public static final String UPLOAD = "/upload";
    public static final String CLAIM = "/claim";
    
    public static final String DATA_NULL_CHK = "RESOURCE_IS_NULL";
    public static final String DATA_NULL_MSG = "`%s` should not be null!";

    public static final String SHIBBOLETH_READ_ACCESS = "https://jans.io/oauth/config/shibboleth.readonly";
    public static final String SHIBBOLETH_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth.write";
    public static final String SHIBBOLETH_DELETE_ACCESS = "https://jans.io/oauth/config/shibboleth.delete";
    public static final String SHIBBOLETH_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth.admin";

    public static final String SHIBBOLETH_CONFIG_READ_ACCESS = "https://jans.io/oauth/config/shibboleth/config.readonly";
    public static final String SHIBBOLETH_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth/config.write";
    public static final String SHIBBOLETH_CONFIG_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth/config.admin";

    public static final String SHIBBOLETH_TR_READ_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.readonly";
    public static final String SHIBBOLETH_TR_WRITE_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.write";
    public static final String SHIBBOLETH_TR_DELETE_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.delete";
    public static final String SHIBBOLETH_TR_ADMIN_ACCESS = "https://jans.io/oauth/config/shibboleth/trust.admin";
    
    public static final String SHIBBOLETH_FILES_UPLOAD = "https://jans.io/oauth/config/shibboleth/files.upload";
    public static final String SHIBBOLETH_FILES_CLAIM = "https://jans.io/oauth/config/shibboleth/files.claim";
    public static final String SHIBBOLETH_FILES_ADMIN = "https://jans.io/oauth/config/shibboleth/files.admin";

}
