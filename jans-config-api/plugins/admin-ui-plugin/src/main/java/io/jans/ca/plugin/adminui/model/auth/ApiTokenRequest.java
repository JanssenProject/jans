package io.jans.ca.plugin.adminui.model.auth;

public class ApiTokenRequest {

    private String ujwt;

    private String refreshToken;

    /**
     * Retrieves the UJWT token value.
     *
     * @return the UJWT string, or {@code null} if not set
     */
    public String getUjwt() {
        return ujwt;
    }

    public void setUjwt(String ujwt) {
        this.ujwt = ujwt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}