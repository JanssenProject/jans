/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

/**
 * @author Javier Rojas Blum Date: 04.26.2012
 */
public class PublicKey {

    private String modulus;
    private String exponent;
    private String x;
    private String y;

    /**
     * Returns the modulus value for the RSA public key. It is represented as the base64url encoding of the value's
     * representation.
     *
     * @return The modulus value for the RSA public key.
     */
    public String getModulus() {
        return modulus;
    }

    /**
     * Sets the modulus value for the RSA public key.
     *
     * @param modulus The modulus value for the RSA public key.
     */
    public void setModulus(String modulus) {
        this.modulus = modulus;
    }

    /**
     * Returns the exponent value for the RSA public key.
     *
     * @return The exponent value for the RSA public key.
     */
    public String getExponent() {
        return exponent;
    }

    /**
     * Sets the exponent value for the RSA public key.
     *
     * @param exponent The exponent value for the RSA public key.
     */
    public void setExponent(String exponent) {
        this.exponent = exponent;
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