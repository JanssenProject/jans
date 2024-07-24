package io.jans.fido2.model.assertion;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.orm.model.fido2.UserVerification;

public class AssertionOptionsGenerate {
    private UserVerification userVerification;
    private String documentDomain;
    private Long timeout;
    private JsonNode extensions;
    private String session_id;

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

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    @Override
    public String toString() {
        return "AssertionOptionsGenerate{" +
                "userVerification=" + userVerification +
                ", documentDomain='" + documentDomain + '\'' +
                ", timeout=" + timeout +
                ", extensions=" + extensions +
                ", session_id='" + session_id + '\'' +
                '}';
    }
}
