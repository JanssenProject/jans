/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

/**
 * @author Javier Rojas Blum
 * @version February 17, 2016
 */
public class PublicKey {

    /**
     * Modulus
     */
    private String n;

    /**
     * Exponent
     */
    private String e;

    private String x;
    private String y;

    /**
     * Returns the modulus value for the RSA public key. It is represented as the base64url encoding of the value's
     * representation.
     *
     * @return The modulus value for the RSA public key.
     */
    public String getN() {
        return n;
    }

    /**
     * Sets the modulus value for the RSA public key.
     *
     * @param n The modulus value for the RSA public key.
     */
    public void setN(String n) {
        this.n = n;
    }

    /**
     * Returns the exponent value for the RSA public key.
     *
     * @return The exponent value for the RSA public key.
     */
    public String getE() {
        return e;
    }

    /**
     * Sets the exponent value for the RSA public key.
     *
     * @param e The exponent value for the RSA public key.
     */
    public void setE(String e) {
        this.e = e;
    }

    /**
     * Returns the x member that contains the x coordinate for the elliptic curve point. It is represented as the
     * base64url encoding of the coordinate's big endian representation.
     *
     * @return The x member that contains the x coordinate for the elliptic curve point.
     */
    public String getX() {
        return x;
    }

    /**
     * Sets the x member that contains the x coordinate for the elliptic curve point.
     *
     * @param x The x member that contains the x coordinate for the elliptic curve point.
     */
    public void setX(String x) {
        this.x = x;
    }

    /**
     * Returns the y member that contains the x coordinate for the elliptic curve point. It is represented as the
     * base64url encoding of the coordinate's big endian representation.
     *
     * @return The y member that contains the x coordinate for the elliptic curve point.
     */
    public String getY() {
        return y;
    }

    /**
     * Sets the y member that contains the y coordinate for the elliptic curve point.
     *
     * @param y The y member that contains the y coordinate for the elliptic curve point.
     */
    public void setY(String y) {
        this.y = y;
    }
}