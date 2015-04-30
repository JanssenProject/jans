package org.xdi.oxd.licenser.server;

import net.nicholaswilliams.java.licensing.SignedLicense;
import net.nicholaswilliams.java.licensing.encryption.PasswordProvider;
import net.nicholaswilliams.java.licensing.encryption.PrivateKeyDataProvider;
import net.nicholaswilliams.java.licensing.exception.KeyNotFoundException;
import org.xdi.oxd.license.client.data.LicenseResponse;
import org.xdi.oxd.license.client.lib.ALicense;
import org.xdi.oxd.license.client.lib.LicenseSerializationUtilities;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 04/09/2014
 */

public class LicenseGenerator {

    public SignedLicense generateSignedLicense(final LicenseGeneratorInput input) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PrivateKeyDataProvider privateKeyDataProvider = new PrivateKeyDataProvider() {
            @Override
            public byte[] getEncryptedPrivateKeyData() throws KeyNotFoundException {
                return input.getPrivateKey();
            }
        };
        PasswordProvider privatePasswordProvider = new PasswordProvider() {
            @Override
            public char[] getPassword() {
                return input.getPrivatePassword().toCharArray();
            }
        };
        LicenseCreator licenseCreator = new LicenseCreator(privateKeyDataProvider, privatePasswordProvider);

        // generate license
        ALicense license = new ALicense.Builder().
                withSubject(input.getMetadata()).
                withHolder("Gluu").
                withGoodBeforeDate(input.getExpiredAt().getTime()).
                build();

        return licenseCreator.signLicense(license, input.getLicensePassword().toCharArray());
    }

    public LicenseResponse generate(final LicenseGeneratorInput input) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final SignedLicense signedLicense = generateSignedLicense(input);
        return new LicenseResponse(LicenseSerializationUtilities.serialize(signedLicense));
    }


}
