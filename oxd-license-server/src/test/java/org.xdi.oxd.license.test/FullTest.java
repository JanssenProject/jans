package org.xdi.oxd.license.test;

import net.nicholaswilliams.java.licensing.LicenseProvider;
import net.nicholaswilliams.java.licensing.ObjectSerializer;
import net.nicholaswilliams.java.licensing.SignedLicense;
import net.nicholaswilliams.java.licensing.encryption.PasswordProvider;
import net.nicholaswilliams.java.licensing.encryption.PublicKeyDataProvider;
import net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGenerator;
import net.nicholaswilliams.java.licensing.exception.KeyNotFoundException;
import org.testng.annotations.Test;
import org.xdi.oxd.license.client.js.LicenseType;
import org.xdi.oxd.license.client.lib.ALicense;
import org.xdi.oxd.license.client.lib.ALicenseManager;
import org.xdi.oxd.licenser.server.LicenseGenerator;
import org.xdi.oxd.licenser.server.LicenseGeneratorInput;
import org.xdi.oxd.licenser.server.LicenseSerializationUtilities;

import javax.xml.bind.DatatypeConverter;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/09/2014
 */

public class FullTest {

    @Test
    public void test() throws NoSuchAlgorithmException, InvalidKeySpecException {

        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        KeyPair keyPair = generator.generateKeyPair();
        final PrivateKey privateKey = keyPair.getPrivate();
        final PublicKey publicKey = keyPair.getPublic();

        KeyFactory fact = KeyFactory.getInstance("RSA");
//        RSAPublicKeySpec pub = fact.getKeySpec(publicKey, RSAPublicKeySpec.class);
//        RSAPrivateKeySpec priv = fact.getKeySpec(privateKey, RSAPrivateKeySpec.class);

        // init license creator
        final String privatePassword = "private password";
        final String publicPassword = "public password";
        final String licensePassword = "license password";

        // generate license
//        ALicense license = new ALicense.Builder().
//                withProductKey("5565-1039-AF89-GGX7-TN31-14AL").
//                withHolder("Customer Name").
//                withGoodBeforeDate(new Date().getTime() + 100).
//                addFeature("GluuFeature").
//                addFeature("4").
//                addFeature("FEATURE2", new Date().getTime() + 100).
//                build();

        LicenseGeneratorInput input = new LicenseGeneratorInput();
        input.setCustomerName("Customer Name");
        input.setPrivateKey(LicenseSerializationUtilities.writeEncryptedPrivateKey(privateKey, privatePassword.toCharArray()));
        input.setPublicKey(LicenseSerializationUtilities.writeEncryptedPublicKey(publicKey, publicPassword.toCharArray()));
        input.setLicensePassword(licensePassword);
        input.setPrivatePassword(privatePassword);
        input.setPublicPassword(publicPassword);
        input.setThreadsCount(5);
        input.setLicenseType(LicenseType.FREE.name());
        input.setExpiredAt(new Date()); // todo !!!

        LicenseGenerator licenseGenerator = new LicenseGenerator();
        final SignedLicense signedLicenseObject = licenseGenerator.generateSignedLicense(input);

        final byte[] serializedLicense = new ObjectSerializer().writeObject(signedLicenseObject);

        final String encodedLicense = DatatypeConverter.printBase64Binary(serializedLicense);

        System.out.println(encodedLicense);

        // CLIENT =========================
        // validate license
        LicenseProvider licenseProvider = new LicenseProvider() {
            @Override
            public SignedLicense getLicense(Object context) {
                return signedLicenseObject;
            }
        };
        PublicKeyDataProvider publicKeyDataProvider = new PublicKeyDataProvider() {
            @Override
            public byte[] getEncryptedPublicKeyData() throws KeyNotFoundException {
                return LicenseSerializationUtilities.writeEncryptedPublicKey(publicKey, publicPassword.toCharArray());
            }
        };
        PasswordProvider publicKeyPasswordProvider = new PasswordProvider() {
            @Override
            public char[] getPassword() {
                return publicPassword.toCharArray();
            }
        };
        PasswordProvider licensePasswordProvider = new PasswordProvider() {
            @Override
            public char[] getPassword() {
                return licensePassword.toCharArray();
            }
        };

        ALicenseManager manager = new ALicenseManager(publicKeyDataProvider, publicKeyPasswordProvider,
                licenseProvider, licensePasswordProvider);

        ALicense license = manager.decryptAndVerifyLicense(signedLicenseObject);// DECRYPT signed license
        manager.validateLicense(license);
        System.out.println("License is valid!");

        int seats = license.getNumberOfLicenses();
        System.out.println("Seats: " + seats);

        final boolean gluuFeature = manager.hasLicenseForAllFeatures(input.getCustomerName(), "FREE");
        if (!gluuFeature) {
            throw new RuntimeException("Invalid license!");
        }
        System.out.println("GluuFeature is present!");

        // negative test
        final boolean fakeGluuFeature = manager.hasLicenseForAllFeatures(input.getCustomerName(), "fakeGluuFeature");
        if (fakeGluuFeature) {
            throw new RuntimeException("There is feature that we didn't add. Invalid license!");
        }


    }
}
