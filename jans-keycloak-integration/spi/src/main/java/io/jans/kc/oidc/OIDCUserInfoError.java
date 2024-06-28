package io.jans.kc.oidc;

public class OIDCUserInfoError {

    private String code;
    private String description;
    private int httpStatusCode;

    public OIDCUserInfoError(String code, String description, int httpStatusCode) {

        this.code = code;
        this.description = description;
        this.httpStatusCode = httpStatusCode;
    }

    public String code() {

        return this.code;
    }
    
    public String description() {

        return this.description;
    }

    public int httpStatusCode() {

        return this.httpStatusCode;
    }
}
