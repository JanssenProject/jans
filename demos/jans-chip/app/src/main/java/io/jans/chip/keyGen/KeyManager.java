package io.jans.chip.keyGen;

import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;

import javax.security.auth.x500.X500Principal;

public class KeyManager {
    private static final String KEY_ALIAS = "JansChipKeystore";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private KeyPair kp;
    private static KeyManager single_instance = null;

    private KeyManager() {
    }

    public static synchronized KeyManager getInstance() {
        if (single_instance == null)
            single_instance = new KeyManager();

        return single_instance;
    }

    /**
     * The function retrieves the public key from the Android Keystore, generating a new key pair if necessary.
     *
     * @return The method is returning a PublicKey object.
     */
    public PublicKey getPublicKey() {
        try {
            if (!checkKeyExists()) {
                generateKeyPair();
            }
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
            PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
            return publicKey;
        } catch (
                NoSuchAlgorithmException |
                UnrecoverableKeyException |
                 CertificateException |
                 KeyStoreException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The function retrieves the private key from the Android Keystore, generating a new key pair if it doesn't exist.
     *
     * @return The method is returning a PrivateKey object.
     */
    public PrivateKey getPrivateKey() {
        try {
            if (!checkKeyExists()) {
                generateKeyPair();
            }
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
            PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();

            return privateKey;
        } catch (NoSuchAlgorithmException |
                 UnrecoverableKeyException | CertificateException |
                 KeyStoreException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The function generates a key pair using RSA algorithm with specified parameters and returns the public key.
     *
     * @return The method is returning a PublicKey object.
     */
    private PublicKey generateKeyPair() {

        try {

            GregorianCalendar startDate = new GregorianCalendar();
            GregorianCalendar endDate = new GregorianCalendar();
            endDate.add(Calendar.YEAR, 1);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE);

            kpg.initialize(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setCertificateSerialNumber(BigInteger.valueOf(777))       //Serial number used for the self-signed certificate of the generated key pair, default is 1
                    .setCertificateSubject(new X500Principal("CN=" + KEY_ALIAS))     //Subject used for the self-signed certificate of the generated key pair, default is CN=fake
                    .setDigests(KeyProperties.DIGEST_SHA256)                         //Set of digests algorithms with which the key can be used
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1) //Set of padding schemes with which the key can be used when signing/verifying
                    .setCertificateNotBefore(startDate.getTime())                         //Start of the validity period for the self-signed certificate of the generated, default Jan 1 1970
                    .setCertificateNotAfter(endDate.getTime())                            //End of the validity period for the self-signed certificate of the generated key, default Jan 1 2048
                    //.setUserAuthenticationRequired(true)                             //Sets whether this key is authorized to be used only if the user has been authenticated, default false
                    .setKeySize(2048)
                    .setUserAuthenticationValidityDurationSeconds(30)
                    .build());

            kp = kpg.generateKeyPair();
            Log.d("Key pair successfully generated:: Public Key", kp.getPublic().toString());
            Log.d("Key pair successfully generated:: Public Key", Base64.encodeToString(kp.getPublic().getEncoded(), Base64.NO_WRAP));

            return kp.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * The function checks if a private and public key pair exists in the Android Keystore.
     *
     * @return The method is returning a boolean value. It returns true if both the private key and public key exist in the
     * keystore, and false otherwise.
     */
    private boolean checkKeyExists() {
        //We get the Keystore instance
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            //We get the private and public key from the keystore if they exists
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
            PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
            return privateKey != null && publicKey != null;
        } catch (KeyStoreException | UnrecoverableKeyException | CertificateException |
                 IOException | NoSuchAlgorithmException | NullPointerException e) {
            return false;
            //throw new RuntimeException(e);
        }
    }

    /**
     * The function generates a signature for the given data using a private key stored in the Android Keystore.
     *
     * @param data The "data" parameter is a byte array that represents the data that you want to sign.
     * @return The method is returning a String representation of the signature generated from the provided data.
     */
    public String signData(byte[] data) {
        KeyStore keyStore = null;
        try {
            if (!checkKeyExists()) {
                generateKeyPair();
            }
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            //We get the private and public key from the keystore if they exists
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
            //PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(privateKey);
            s.update(data);
            byte[] signature = s.sign();
            return new String(signature, StandardCharsets.UTF_8);
        } catch (KeyStoreException | UnrecoverableKeyException | CertificateException |
                 IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The function takes a public key and returns a JWK (JSON Web Key) representation of the key.
     *
     * @param publicKey The publicKey parameter is of type PublicKey and represents the public key that you want to convert
     * to a JWK (JSON Web Key) format.
     * @return The method is returning a JWK (JSON Web Key) object.
     */
    public static JWK getPublicKeyJWK(PublicKey publicKey) {
        JWK jwk = new RSAKey.Builder((RSAPublicKey) publicKey)
                .build();
        return jwk;
    }
}
