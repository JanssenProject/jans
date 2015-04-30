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

//    final PrivateKey privateKey = keyPair.getPrivate();
//    final PublicKey publicKey = keyPair.getPublic();
//
//    KeyFactory fact = KeyFactory.getInstance("RSA");
//    RSAPublicKeySpec pub = fact.getKeySpec(publicKey, RSAPublicKeySpec.class);
//    RSAPrivateKeySpec priv = fact.getKeySpec(privateKey, RSAPrivateKeySpec.class);

    //        final BigInteger publicModulus = pub.getModulus();
    //        final BigInteger publicExponent = pub.getPublicExponent();
    //
    //        final BigInteger privateModulus = priv.getModulus();
    //        final BigInteger privateExponent = priv.getPrivateExponent();
}
