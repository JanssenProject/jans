/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.encryption;

/**
 * @author Javier Rojas Blum Date: 12.03.2012
 */
public enum KeyEncryptionAlgorithm {

    RSA1_5("RSA1_5", "RSA", "RSA/ECB/PKCS1Padding"),
    RSA_OAEP("RSA-OAEP", "RSA", "RSA/ECB/OAEPWithSHA1AndMGF1Padding"),
    A128KW("A128KW"),
    A256KW("A256KW");
    //DIR("dir"), // Not supported
    //ECDH_ES("ECDH-ES"), // Not supported
    //ECDH_ES_PLUS_A128KW("ECDH-ES+A128KW"), // Not supported
    //ECDH_ES_A256KW("ECDH-ES+A256KW"); // Not supported

    private final String name;
    private final String family;
    private final String algorithm;

    private KeyEncryptionAlgorithm(String name) {
        this.name = name;
        this.family = null;
        this.algorithm = null;
    }

    private KeyEncryptionAlgorithm(String name, String family, String algorithm) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
    }

    public String getName() {
        return name;
    }

    public String getFamily() {
        return family;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public static KeyEncryptionAlgorithm fromName(String name) {
        if (name != null) {
            for (KeyEncryptionAlgorithm a : KeyEncryptionAlgorithm.values()) {
                if (name.equals(a.name)) {
                    return a;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}