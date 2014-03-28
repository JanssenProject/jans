package org.xdi.oxauth.model.jwt;

/**
 * @author Javier Rojas Blum Date: 11.03.2012
 */
public enum JwtType {

    JWT, JWS, JWE;

    /**
     * Returns the corresponding {@link JwtType} for a parameter.
     *
     * @param param The parameter.
     * @return The corresponding JWT Type if found, otherwise <code>null</code>.
     */
    public static JwtType fromString(String param) {
        if (param != null) {
            for (JwtType t : JwtType.values()) {
                if (param.equals(t.toString())) {
                    return t;
                }
            }
        }
        return null;
    }
}