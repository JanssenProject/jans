package io.jans.fido2.model.assertion;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.model.common.SuperGluuSupport;
import io.jans.orm.model.fido2.UserVerification;

public class AssertionOptions extends SuperGluuSupport {
    private String username;
    private UserVerification userVerification;
    private String documentDomain;
    private JsonNode extensions;
    private Long timeout;
    private String session_id;

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

    public String getDocumentDomain() {
        return documentDomain;
    }

    public void setDocumentDomain(String documentDomain) {
        this.documentDomain = documentDomain;
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

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    @Override
    public String toString() {
        return "AssertionOptions{" +
                "username='" + username + '\'' +
                ", userVerification=" + userVerification +
                ", documentDomain='" + documentDomain + '\'' +
                ", extensions='" + extensions + '\'' +
                ", timeout=" + timeout +
                '}';
    }
}
