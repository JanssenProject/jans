package org.xdi.oxauth.model.jwk;

/**
 * @author Javier Rojas Date: 11.15.2011
 */
public enum Use {

    /**
     * Use this constant when the key is being used for signature.
     */
    SIGNATURE("sig"),
    /**
     * Use this constant when the key is being used for encryption.
     */
    ENCRYPTION("enc");

    private final String paramName;

    private Use(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link Use} for a parameter use of the JWK endpoint.
     *
     * @param param The use parameter.
     * @return The corresponding use if found, otherwise <code>null</code>.
     */
    public static Use fromString(String param) {
        if (param != null) {
            for (Use rt : Use.values()) {
                if (param.equals(rt.paramName)) {
                    return rt;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name.
     *
     * @return The string representation of the object.
     */
    @Override
    public String toString() {
        return paramName;
    }
}