package org.gluu.oxd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OxdToHttpConfiguration extends Configuration {
    @NotEmpty
    private String defaultName = "Stranger";

    @NotEmpty
    private String defaultHost = "localhost";

    @NotEmpty
    private String defaultPort = "8099";

    @NotEmpty
    private String defaultOpHost = "https://ce-dev3.gluu.org";


    @NotEmpty
    private String defaultAuthorizationRedirectUrl = "https://client.example.com/cb";

    @NotEmpty
    private String defaultRedirectUrl = "https://client.example.com/cb";

    @NotEmpty
    private String defaultLogoutUrl = "https://client.example.com/logout";

    @NotEmpty
    private String defaultPostLogoutRedirectUrl = "https://client.example.com/cb/logout";

    @NotEmpty
    private String defaultUserID = "test_user";

    @NotEmpty
    private String defaultUserSecret = "test_user_password";


    @JsonProperty
    public String getDefaultHost() {
        return defaultHost;
    }
    @NotEmpty
    private String defaultIntrospectionEndPointHTTP = defaultOpHost + "/oxauth/seam/resource/restv1/introspection";

    @JsonProperty
    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }

    @JsonProperty
    public String getDefaultPort() {
        return defaultPort;
    }

    @JsonProperty
    public void setDefaultPort(String defaultPort) {
        this.defaultPort = defaultPort;
    }

    @JsonProperty
    public String getDefaultOpHost() {
        return defaultOpHost;
    }

    @JsonProperty
    public void setDefaultOpHost(String defaultOpHost) {
        this.defaultOpHost = defaultOpHost;
    }

    @JsonProperty
    public String getDefaultAuthorizationRedirectUrl() {
        return defaultAuthorizationRedirectUrl;
    }


    @JsonProperty
    public void setDefaultAuthorizationRedirectUrl(String defaultAuthorizationRedirectUrl) {
        this.defaultAuthorizationRedirectUrl = defaultAuthorizationRedirectUrl;
    }
    @JsonProperty
    public String getDefaultRedirectUrl() {
        return defaultRedirectUrl;
    }

    @JsonProperty
    public void setDefaultRedirectUrl(String defaultRedirectUrl) {
        this.defaultRedirectUrl = defaultRedirectUrl;
    }

    @JsonProperty
    public String getDefaultLogoutUrl() {
        return defaultLogoutUrl;

}
    @JsonProperty
    public void setDefaultLogoutUrl(String defaultLogoutUrl) {
        this.defaultLogoutUrl = defaultLogoutUrl;
    }

    @JsonProperty
    public String getDefaultPostLogoutRedirectUrl() {
        return defaultPostLogoutRedirectUrl;
    }

    @JsonProperty
    public void setDefaultPostLogoutRedirectUrl(String defaultPostLogoutRedirectUrl) {
        this.defaultPostLogoutRedirectUrl = defaultPostLogoutRedirectUrl;
    }


    @JsonProperty
    public String getDefaultUserID() {
        return defaultUserID;
    }

    @JsonProperty
    public void setDefaultUserID(String defaultUserID) {
        this.defaultUserID = defaultUserID;
    }

    @JsonProperty
    public String getDefaultUserSecret() {
        return defaultUserSecret;
    }

    @JsonProperty
    public void setDefaultUserSecret(String defaultUserSecret) {
        this.defaultUserSecret = defaultUserSecret;
    }

    @JsonProperty
    public String getDefaultIntrospectionEndPointHTTP() {
        return defaultIntrospectionEndPointHTTP;
    }

    @JsonProperty
    public void setDefaultIntrospectionEndPointHTTP(String defaultIntrospectionEndPointHTTP) {
        this.defaultIntrospectionEndPointHTTP = defaultIntrospectionEndPointHTTP;
    }

    @JsonProperty
    public String getDefaultName() {
        return defaultName;
    }

    @JsonProperty
    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }
}
