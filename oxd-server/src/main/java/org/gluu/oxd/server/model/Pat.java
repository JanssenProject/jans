package org.gluu.oxd.server.model;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/06/2016
 */

public class Pat extends Token {

    public Pat() {
    }

    public Pat(String token, String refreshToken, int expiresIn) {
        super(token, refreshToken, expiresIn);
    }
}
