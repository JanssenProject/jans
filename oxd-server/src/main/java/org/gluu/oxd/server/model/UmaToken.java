package org.gluu.oxd.server.model;

/**
 * UmaToken used for both AAT and PAT
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public abstract class UmaToken {

    private String token;
    private String refreshToken;
    private int expiresIn;

    public UmaToken() {
    }

    public UmaToken(String token, String refreshToken, int expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UmaToken umaToken = (UmaToken) o;

        if (token != null ? !token.equals(umaToken.token) : umaToken.token != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return token != null ? token.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("UmaToken");
        sb.append("{token='").append(token).append('\'');
        sb.append(", refreshToken='").append(refreshToken).append('\'');
        sb.append(", expiresIn=").append(expiresIn);
        sb.append('}');
        return sb.toString();
    }
}
