/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public enum EllipticEdvardsCurve {

    P_256("P-256", "secp256r1", "1.2.840.10045.3.1.7"),
    P_256K("P-256K", "secp256k1", "1.3.132.0.10"),
    P_384("P-384", "secp384r1", "1.3.132.0.34"),
    P_521("P-521", "secp521r1", "1.3.132.0.35"),
    ED_25519("Ed25519", "Ed25519", "oid: 1.3.101.112"), // "oid: ", as Static Analyzer estimate "1.3.101.112" as string, that contains ip address        
    ED_448("Ed448", "Ed448", "oid: 1.3.101.113"); // "oid: ", as Static Analyzer estimate "1.3.101.113" as string, that contains ip address

    private final String name;
    private final String alias;
    private final String oid;

    EllipticEdvardsCurve(String name, String alias, String oid) {
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
     * Returns the corresponding {@link EllipticEdvardsCurve} for a parameter crv of the JWK endpoint.
     *
     * @param param The crv parameter.
     * @return The corresponding curve if found, otherwise <code>null</code>.
     */
    @JsonCreator
    public static EllipticEdvardsCurve fromString(String param) {
        if (param != null) {
            for (EllipticEdvardsCurve ec : EllipticEdvardsCurve.values()) {
                if (param.equalsIgnoreCase(ec.getName()) || param.equalsIgnoreCase(ec.getAlias())) {
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
