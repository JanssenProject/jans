package io.jans.fido2.model.assertion;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.model.common.PublicKeyCredentialDescriptor;
import io.jans.orm.model.fido2.UserVerification;
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssertionOptions  {
    private String username;
    private UserVerification userVerification;
    private String origin;
    private JsonNode extensions;
    private Long timeout;
    @JsonProperty(value = "session_id")
    private String sessionId;
    
    // 1. allowCredentials (An array of objects used to restrict the list of acceptable credentials. An empty array indicates that any credential is acceptable.)
    private List<PublicKeyCredentialDescriptor> allowCredentials;
    // rawid
    private String credentialId;
    

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserVerification getUserVerification() {
        return userVerification;
    }

    public void setUserVerification(UserVerification userVerification) {
        this.userVerification = userVerification;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public JsonNode getExtensions() {
        return extensions;
    }

    public void setExtensions(JsonNode extensions) {
        this.extensions = extensions;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCredentialId() {
		return credentialId;
	}

	public void setCredentialId(String credentialId) {
		this.credentialId = credentialId;
	}

	public List<PublicKeyCredentialDescriptor> getAllowCredentials() {
		return allowCredentials;
	}

	public void setAllowCredentials(List<PublicKeyCredentialDescriptor> allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	@Override
	public String toString() {
		return "AssertionOptions [username=" + username + ", userVerification=" + userVerification + ", origin="
				+ origin + ", extensions=" + extensions + ", timeout=" + timeout + ", sessionId=" + sessionId
				+ ", allowCredentials=" + allowCredentials.toString() + ", credentialId=" + credentialId + "]";
	}

	

	
}
