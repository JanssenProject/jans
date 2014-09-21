package org.xdi.oxd.licenser.server;

import com.google.inject.Inject;
import net.nicholaswilliams.java.licensing.License;
import net.nicholaswilliams.java.licensing.ObjectSerializer;
import net.nicholaswilliams.java.licensing.SignedLicense;
import net.nicholaswilliams.java.licensing.encryption.PasswordProvider;
import net.nicholaswilliams.java.licensing.encryption.PrivateKeyDataProvider;
import net.nicholaswilliams.java.licensing.exception.KeyNotFoundException;
import net.nicholaswilliams.java.licensing.licensor.LicenseCreator;
import net.nicholaswilliams.java.licensing.licensor.LicenseCreatorProperties;

import javax.xml.bind.DatatypeConverter;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 04/09/2014
 */

public class LicenseGenerator {

    @Inject
    KeyPairService keyPairGenerator;

    public org.xdi.oxd.license.client.data.License generate() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyPair keyPair = keyPairGenerator.generate();
        final PrivateKey privateKey = keyPair.getPrivate();
        final PublicKey publicKey = keyPair.getPublic();

        KeyFactory fact = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pub = fact.getKeySpec(publicKey, RSAPublicKeySpec.class);
        RSAPrivateKeySpec priv = fact.getKeySpec(privateKey, RSAPrivateKeySpec.class);

        //        final BigInteger publicModulus = pub.getModulus();
        //        final BigInteger publicExponent = pub.getPublicExponent();
        //
        //        final BigInteger privateModulus = priv.getModulus();
        //        final BigInteger privateExponent = priv.getPrivateExponent();

        // init license creator
        final char[] privatePassword = "privatepassword".toCharArray();
        final char[] publicPassword = "publicpassword".toCharArray();
        final char[] licensePassword = "licensepassword".toCharArray();

        LicenseCreatorProperties.setPrivateKeyDataProvider(new PrivateKeyDataProvider() {
            @Override
            public byte[] getEncryptedPrivateKeyData() throws KeyNotFoundException {
                return LicenseSerializationUtilities.writeEncryptedPrivateKey(privateKey, privatePassword);
            }
        });
        LicenseCreatorProperties.setPrivateKeyPasswordProvider(new PasswordProvider() {
            @Override
            public char[] getPassword() {
                return privatePassword;
            }
        });
        LicenseCreator.getInstance();

        // generate license
        License license = new License.Builder().
                withProductKey("5565-1039-AF89-GGX7-TN31-14AL").
                withHolder("Customer Name").
                withGoodBeforeDate(new Date().getTime() + 100).
                addFeature("GluuFeature").
                addFeature("FEATURE2", new Date().getTime() + 100).
                build();

        final SignedLicense signedLicense = LicenseCreator.getInstance().signLicense(license, licensePassword);
        final byte[] serializedLicense = new ObjectSerializer().writeObject(signedLicense);

        final String encodedLicense = DatatypeConverter.printBase64Binary(serializedLicense);
        return new org.xdi.oxd.license.client.data.License(encodedLicense);
    }


}
