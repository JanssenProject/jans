package org.xdi.oxd.licenser.server;

import net.nicholaswilliams.java.licensing.DataSignatureManager;
import net.nicholaswilliams.java.licensing.ObjectSerializer;
import net.nicholaswilliams.java.licensing.SignedLicense;
import net.nicholaswilliams.java.licensing.encryption.Encryptor;
import net.nicholaswilliams.java.licensing.encryption.KeyFileUtilities;
import net.nicholaswilliams.java.licensing.encryption.PasswordProvider;
import net.nicholaswilliams.java.licensing.encryption.PrivateKeyDataProvider;
import net.nicholaswilliams.java.licensing.exception.AlgorithmNotSupportedException;
import net.nicholaswilliams.java.licensing.exception.InappropriateKeyException;
import net.nicholaswilliams.java.licensing.exception.InappropriateKeySpecificationException;
import net.nicholaswilliams.java.licensing.exception.KeyNotFoundException;
import net.nicholaswilliams.java.licensing.exception.ObjectSerializationException;
import org.xdi.oxd.license.client.lib.ALicense;

import java.security.PrivateKey;
import java.util.Arrays;

/**
 * Slightly modified "net.nicholaswilliams.java.licensing.licensor.LicenseCreator" created by Nickolas Williams.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2014
 */

public class LicenseCreator {

    private final PrivateKeyDataProvider privateKeyDataProvider;

    private final PasswordProvider privateKeyPasswordProvider;

    public LicenseCreator(PrivateKeyDataProvider privateKeyDataProvider, PasswordProvider privateKeyPasswordProvider) {
        this.privateKeyDataProvider = privateKeyDataProvider;
        this.privateKeyPasswordProvider = privateKeyPasswordProvider;
    }

    public final SignedLicense signLicense(ALicense license, char[] licensePassword)
            throws AlgorithmNotSupportedException, KeyNotFoundException, InappropriateKeySpecificationException,
            InappropriateKeyException {


        char[] password = this.privateKeyPasswordProvider.getPassword();
        byte[] keyData = this.privateKeyDataProvider.getEncryptedPrivateKeyData();

        PrivateKey key = KeyFileUtilities.readEncryptedPrivateKey(keyData, password);

        Arrays.fill(password, '\u0000');
        Arrays.fill(keyData, (byte) 0);


        byte[] encrypted = Encryptor.encryptRaw(license.serialize(), licensePassword);

        byte[] signature = new DataSignatureManager().signData(key, encrypted);

        SignedLicense signed = new SignedLicense(encrypted, signature);

        Arrays.fill(encrypted, (byte) 0);
        Arrays.fill(signature, (byte) 0);

        return signed;
    }


    public final SignedLicense signLicense(ALicense license)
            throws AlgorithmNotSupportedException, KeyNotFoundException, InappropriateKeySpecificationException,
            InappropriateKeyException {
        return this.signLicense(license, this.privateKeyPasswordProvider.getPassword());
    }


    public final byte[] signAndSerializeLicense(ALicense license, char[] licensePassword)
            throws AlgorithmNotSupportedException, KeyNotFoundException, InappropriateKeySpecificationException,
            InappropriateKeyException, ObjectSerializationException {
        return new ObjectSerializer().writeObject(this.signLicense(license, licensePassword));
    }


    public final byte[] signAndSerializeLicense(ALicense license)
            throws AlgorithmNotSupportedException, KeyNotFoundException, InappropriateKeySpecificationException,
            InappropriateKeyException, ObjectSerializationException {
        return new ObjectSerializer().writeObject(this.signLicense(license));
    }
}
