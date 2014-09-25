package org.xdi.oxd.licenser.server;

import net.nicholaswilliams.java.licensing.License;
import net.nicholaswilliams.java.licensing.ObjectSerializer;
import net.nicholaswilliams.java.licensing.SignedLicense;
import net.nicholaswilliams.java.licensing.encryption.PasswordProvider;
import net.nicholaswilliams.java.licensing.encryption.PrivateKeyDataProvider;
import net.nicholaswilliams.java.licensing.exception.KeyNotFoundException;

import javax.xml.bind.DatatypeConverter;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 04/09/2014
 */

public class LicenseGenerator {

    public org.xdi.oxd.license.client.data.License generate(final LicenseGeneratorInput input) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PrivateKeyDataProvider privateKeyDataProvider =new PrivateKeyDataProvider() {
            @Override
            public byte[] getEncryptedPrivateKeyData() throws KeyNotFoundException {
                return input.getPrivateKey();
            }
        };
        PasswordProvider privatePasswordProvider= new PasswordProvider() {
            @Override
            public char[] getPassword() {
                return input.getPrivatePassword().toCharArray();
            }
        };
        LicenseCreator licenseCreator = new LicenseCreator(privateKeyDataProvider, privatePasswordProvider);

        // generate license
        License license = new License.Builder().
                withProductKey("Gluu").
                withNumberOfLicenses(input.getThreadsCount()).
                withHolder(input.getCustomerName()).
                withGoodBeforeDate(input.getExpiredAt().getTime()).
                addFeature(input.getLicenseType()).
                build();

        final SignedLicense signedLicense = licenseCreator.signLicense(license, input.getLicensePassword().toCharArray());
        final byte[] serializedLicense = new ObjectSerializer().writeObject(signedLicense);

        final String encodedLicense = DatatypeConverter.printBase64Binary(serializedLicense);
        return new org.xdi.oxd.license.client.data.License(encodedLicense);
    }


}
