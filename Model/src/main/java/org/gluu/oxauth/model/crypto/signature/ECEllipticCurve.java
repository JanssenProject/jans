package org.gluu.oxauth.model.crypto.signature;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

/**
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public enum ECEllipticCurve {

    P_256("P-256", "secp256r1", "1.2.840.10045.3.1.7"),
    P_384("P-384", "secp384r1", "1.3.132.0.34"),
    P_521("P-521", "secp521r1", "1.3.132.0.35");

    private final String name;
    private final String alias;
    private final String oid;

    private ECEllipticCurve(String name, String alias, String oid) {
        this.name = name;
        this.alias = alias;
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public String getOid() {
        return oid;
    }

    /**
     * Returns the corresponding {@link ECEllipticCurve} for a parameter crv of the JWK endpoint.
     *
     * @param param The crv parameter.
     * @return The corresponding curve if found, otherwise <code>null</code>.
     */
    @JsonCreator
    public static ECEllipticCurve fromString(String param) {
        if (param != null) {
            for (ECEllipticCurve ec : ECEllipticCurve.values()) {
                if (param.equals(ec.name)) {
                    return ec;
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
    @JsonValue
    public String toString() {
        return name;
    }
}
