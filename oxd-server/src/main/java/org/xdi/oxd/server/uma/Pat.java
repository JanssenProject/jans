package org.xdi.oxd.server.uma;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class Pat {

    private String pat;
    private String refreshToken;
    private int expiresIn;

    public Pat() {
    }

    public Pat(String pat, String refreshToken, int expiresIn) {
        this.pat = pat;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getPat() {
        return pat;
    }

    public void setPat(String pat) {
        this.pat = pat;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }
}
