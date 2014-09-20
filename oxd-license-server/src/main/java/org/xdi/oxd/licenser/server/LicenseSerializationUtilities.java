package org.xdi.oxd.licenser.server;

import net.nicholaswilliams.java.licensing.encryption.Encryptor;
import net.nicholaswilliams.java.licensing.encryption.KeyFileUtilities;
import net.nicholaswilliams.java.licensing.exception.AlgorithmNotSupportedException;
import net.nicholaswilliams.java.licensing.exception.InappropriateKeySpecificationException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 04/09/2014
 */

public class LicenseSerializationUtilities {
    private LicenseSerializationUtilities() {
    }

    public static byte[] writeEncryptedPrivateKey(PrivateKey privateKey, char[] passphrase) {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        return Encryptor.encryptRaw(pkcs8EncodedKeySpec.getEncoded(), passphrase);
    }

    public static byte[] writeEncryptedPublicKey(PublicKey publicKey, char[] passphrase) {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        return Encryptor.encryptRaw(x509EncodedKeySpec.getEncoded(), passphrase);
    }

    public static PrivateKey readEncryptedPrivateKey(byte[] fileContents, char[] passphrase) {
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Encryptor.decryptRaw(fileContents, passphrase));

        try {
            return KeyFactory.getInstance(KeyFileUtilities.keyAlgorithm).generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new AlgorithmNotSupportedException(KeyFileUtilities.keyAlgorithm, e);
        } catch (InvalidKeySpecException e) {
            throw new InappropriateKeySpecificationException(e);
        }
    }

    public static PublicKey readEncryptedPublicKey(byte[] fileContents, char[] passphrase) {
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Encryptor.decryptRaw(fileContents, passphrase));

        try {
            return KeyFactory.getInstance(KeyFileUtilities.keyAlgorithm).generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new AlgorithmNotSupportedException(KeyFileUtilities.keyAlgorithm, e);
        } catch (InvalidKeySpecException e) {
            throw new InappropriateKeySpecificationException(e);
        }
    }

}
