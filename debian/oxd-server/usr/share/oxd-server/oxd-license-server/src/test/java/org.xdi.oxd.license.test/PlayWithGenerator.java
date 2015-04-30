package org.xdi.oxd.license.test;

import net.nicholaswilliams.java.licensing.encryption.KeyFileUtilities;
import net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGenerator;
import org.testng.annotations.Test;

import java.security.KeyPairGenerator;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/11/2014
 */

public class PlayWithGenerator {


    @Test
    public void test() throws Exception {
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        generator.generateKeyPair();


        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(KeyFileUtilities.keyAlgorithm);
        keyGenerator.initialize(2048);
        keyGenerator.generateKeyPair();
    }

}
