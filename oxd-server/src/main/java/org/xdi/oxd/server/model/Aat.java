package org.xdi.oxd.server.model;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/06/2016
 */

public class Aat extends UmaToken {

    public Aat() {
    }

    public Aat(String token, String refreshToken, int expiresIn) {
        super(token, refreshToken, expiresIn);
    }
}
