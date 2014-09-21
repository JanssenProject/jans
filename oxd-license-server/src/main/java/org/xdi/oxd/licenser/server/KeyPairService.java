package org.xdi.oxd.licenser.server;

import net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGenerator;

import java.security.KeyPair;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/09/2014
 */

public class KeyPairService {

    public KeyPair generate() {
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        return generator.generateKeyPair();
    }
}
