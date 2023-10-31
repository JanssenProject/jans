package io.jans.configapi.plugin.keycloak.idp.broker.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpAppConfiguration implements Configuration {

    private String applicationName;
    private String trustedIdpDn;
    private boolean idpEnabled;
    private String idpRootDir;
	private String idpTempDir;
    private String spMetadataFilePattern;
	private String spMetadataFile;
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String grantType;
    private String username;
    private String password;
     
}
