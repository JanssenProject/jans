package io.jans.fido2.model.assertion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.orm.model.fido2.UserVerification;
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssertionOptions  {
    private String username;
    private UserVerification userVerification;
    private String origin;
    private JsonNode extensions;
    private Long timeout;
    @JsonProperty(value = "session_id")
    private String sessionId;

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

    @Override
    public String toString() {
        return "AssertionOptions{" +
                "username='" + username + '\'' +
                ", userVerification=" + userVerification +
                ", origin='" + origin + '\'' +
                ", extensions='" + extensions + '\'' +
                ", timeout=" + timeout +
                '}';
    }
}
