package org.xdi.oxd.licenser.server;

import net.nicholaswilliams.java.licensing.License;
import net.nicholaswilliams.java.licensing.LicenseManager;
import net.nicholaswilliams.java.licensing.LicenseManagerProperties;
import net.nicholaswilliams.java.licensing.LicenseProvider;
import net.nicholaswilliams.java.licensing.ObjectSerializer;
import net.nicholaswilliams.java.licensing.SignedLicense;
import net.nicholaswilliams.java.licensing.encryption.PasswordProvider;
import net.nicholaswilliams.java.licensing.encryption.PrivateKeyDataProvider;
import net.nicholaswilliams.java.licensing.encryption.PublicKeyDataProvider;
import net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGenerator;
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
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        KeyPair keyPair = generator.generateKeyPair();
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
        final char[] privatePassword = "private password".toCharArray();
        final char[] publicPassword = "public password".toCharArray();
        final char[] licensePassword = "license password".toCharArray();

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

        System.out.println(encodedLicense);

        // CLIENT =========================
        // validate license
        LicenseManagerProperties.setLicenseProvider(new LicenseProvider() {
            @Override
            public SignedLicense getLicense(Object context) {
                return signedLicense;
            }
        });
        LicenseManagerProperties.setPublicKeyDataProvider(new PublicKeyDataProvider() {
            @Override
            public byte[] getEncryptedPublicKeyData() throws KeyNotFoundException {
                return LicenseSerializationUtilities.writeEncryptedPublicKey(publicKey, publicPassword);
            }
        });
        LicenseManagerProperties.setPublicKeyPasswordProvider(new PasswordProvider() {
            @Override
            public char[] getPassword() {
                return publicPassword;
            }
        });
        LicenseManagerProperties.setLicensePasswordProvider(new PasswordProvider() {
            @Override
            public char[] getPassword() {
                return licensePassword;
            }
        });
        LicenseManager manager = LicenseManager.getInstance();

        manager.validateLicense(license);
        System.out.println("License is valid!");

        int seats = license.getNumberOfLicenses();
        System.out.println("Seats: " + seats);

        final boolean gluuFeature = manager.hasLicenseForAllFeatures("Customer Name", "GluuFeature");
        if (!gluuFeature) {
            throw new RuntimeException("Invalid license!");
        }
        System.out.println("GluuFeature is present!");

        // negative test
        final boolean fakeGluuFeature = manager.hasLicenseForAllFeatures("Customer Name", "fakeGluuFeature");
        if (fakeGluuFeature) {
            throw new RuntimeException("There is feature that we didn't add. Invalid license!");
        }


    }
}
