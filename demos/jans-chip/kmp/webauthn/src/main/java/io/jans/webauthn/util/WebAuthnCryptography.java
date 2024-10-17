package io.jans.webauthn.util;

import android.util.Log;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class WebAuthnCryptography {
    private CredentialSafe credentialSafe;

    public WebAuthnCryptography(CredentialSafe safe) {
        this.credentialSafe = safe;
    }

    /**
     * Generate a signature object to be unlocked via biometric prompt
     * This signature object should be passed down to performSignature
     *
     * @return Signature that is generated
     */
    public static Signature generateSignatureObject(PrivateKey privateKey) {
        Signature sig = null;
        try {
            sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(privateKey);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            Log.d("WebAuthnCryptography", "couldn't perform signature: " + e);
        }
        return sig;
    }

    /**
     * Perform a signature over an arbitrary byte array.
     *
     * @param sig        Signature object with which to perform the signature, or null to create it
     *                   on the fly.
     * @param privateKey The private key with which to sign.
     * @param data       The data to be signed.
     * @return A byte array representing the signature in ASN.1 DER Ecdsa-Sig-Value format.
     */
    public byte[] performSignature(PrivateKey privateKey, byte[] data, Signature sig) {
        try {
            if (sig == null) {
                sig = Signature.getInstance("SHA256withECDSA");
                sig.initSign(privateKey);
            }
            sig.update(data);
            return sig.sign();
        } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
            Log.d("WebAuthnCryptography", "couldn't perform signature: " + e);
            return null;
        }
    }

    /**
     * Verify a signature.
     *
     * @param publicKey The key with which to verify the signature.
     * @param data      The data that was signed.
     * @param signature The signature in ASN.1 DER Ecdsa-Sig-Value format.
     * @return true iff the signature is valid
     */
    public boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signature) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            Log.d("WebAuthnCryptography", "couldn't perform signature validation", e);
            return false;
        }
    }

    /**
     * Hash a string with SHA-256.
     *
     * @param data The string to be hashed.
     * @return A byte array containing the hash.
     */
    public static byte[] sha256(String data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.d("WebAuthnCryptography", "couldn't hash data", e);
        }
        md.update(data.getBytes());
        return md.digest();
    }
}
