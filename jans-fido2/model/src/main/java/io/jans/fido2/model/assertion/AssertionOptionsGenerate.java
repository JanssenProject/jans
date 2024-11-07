package io.jans.fido2.model.assertion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.jans.orm.model.fido2.UserVerification;
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssertionOptionsGenerate {
    private UserVerification userVerification;
    private String documentDomain;
    private Long timeout;
    private JsonNode extensions;
    @JsonProperty(value = "session_id")
    private String sessionId;

    public UserVerification getUserVerification() {
        return userVerification;
    }

    public void setUserVerification(UserVerification userVerification) {
        this.userVerification = userVerification;
    }

    public String getDocumentDomain() {
        return documentDomain;
    }

    public void setDocumentDomain(String documentDomain) {
        this.documentDomain = documentDomain;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public JsonNode getExtensions() {
        return extensions;
    }

    public void setExtensions(JsonNode extensions) {
        this.extensions = extensions;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "AssertionOptionsGenerate{" +
                "userVerification=" + userVerification +
                ", documentDomain='" + documentDomain + '\'' +
                ", timeout=" + timeout +
                ", extensions=" + extensions +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
