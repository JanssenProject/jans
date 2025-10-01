package io.jans.webauthn.util;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import io.jans.webauthn.exceptions.VirgilException;
import io.jans.webauthn.models.PublicKeyCredentialSource;
import io.jans.webauthn.util.database.CredentialDatabase;


/**
 * CredentialSafe uses the Android KeyStore to generate and store
 * ES256 keys that are hardware-backed.
 * <p>
 * These keys can optionally be protected with "Strongbox keymaster" protection and user
 * authentication on supported hardware.
 */
public class CredentialSafe {
    private static final String KEYSTORE_TYPE = "AndroidKeyStore";
    private static final String CURVE_NAME = "secp256r1";
    private KeyStore keyStore;
    private boolean authenticationRequired;
    private boolean strongboxRequired;
    private CredentialDatabase db;

    /**
     * Construct a CredentialSafe that requires user authentication and strongbox backing.
     *
     * @param ctx The application context
     * @throws VirgilException
     */
    public CredentialSafe(Context ctx) throws VirgilException {
        this(ctx, true, true);
    }

    /**
     * Construct a CredentialSafe with configurable user authentication / strongbox choices.
     *
     * @param ctx                    The application context
     * @param authenticationRequired Whether user will be required to use biometrics to allow each
     *                               use of keys generated (requires fingerprint enrollment).
     * @param strongboxRequired      Require keys to be backed by the "Strongbox Keymaster" HSM.
     *                               Requires hardware support.
     * @throws VirgilException
     */
    public CredentialSafe(Context ctx, boolean authenticationRequired, boolean strongboxRequired) throws VirgilException {
        try {
            keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(null);
        } catch (KeyStoreException | CertificateException |
                NoSuchAlgorithmException | IOException e) {
            throw new VirgilException("couldn't access keystore", e);
        }

        this.authenticationRequired = authenticationRequired;
        this.strongboxRequired = strongboxRequired;
        this.db = CredentialDatabase.getDatabase(ctx);
    }

    /**
     * Determine if user verification (by the WebAuthn definition) is supported.
     *
     * @return status of user verification requirement
     */
    public boolean supportsUserVerification() {
        return this.authenticationRequired;
    }


    /**
     * Generate a new ES256 keypair (COSE algorithm -7, ECDSA + SHA-256 over the NIST P-256 curve).
     *
     * @param alias The alias used to identify this keypair in the keystore. Needed to use key
     *              in the future.
     * @return The KeyPair object representing the newly generated keypair.
     * @throws VirgilException
     */
    private KeyPair generateNewES256KeyPair(String alias) throws VirgilException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(new ECGenParameterSpec(CURVE_NAME))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(this.authenticationRequired) // fingerprint or similar
                .setUserConfirmationRequired(false) // TODO: Decide if we support Android Trusted Confirmations
                .setInvalidatedByBiometricEnrollment(false)
                //.setIsStrongBoxBacked(this.strongboxRequired)
                .build();
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_TYPE);
            keyPairGenerator.initialize(spec);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return keyPair;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new VirgilException("couldn't generate key pair: " + e.toString());
        }
    }

    /**
     * Generate and save new credential with an ES256 keypair.
     *
     * @param rpEntityId      The relying party's identifier
     * @param userHandle      A unique ID for the user
     * @param userDisplayName A human-readable username for the user
     * @return A PublicKeyCredentialSource object corresponding to the new keypair and its associated
     * rpId, credentialId, etc.
     * @throws VirgilException
     */
    public PublicKeyCredentialSource generateCredential(@NonNull String rpEntityId, byte[] userHandle, String userDisplayName) throws VirgilException {
        PublicKeyCredentialSource credentialSource = new PublicKeyCredentialSource(rpEntityId, userHandle, userDisplayName);
        generateNewES256KeyPair(credentialSource.keyPairAlias); // return not captured -- will retrieve credential by alias
        db.credentialDao().insert(credentialSource);
        return credentialSource;
    }

    public void deleteCredential(PublicKeyCredentialSource credentialSource) {
        db.credentialDao().delete(credentialSource);
    }


    /**
     * Get keys belonging to this RP ID.
     *
     * @param rpEntityId rpEntity.id from WebAuthn spec.
     * @return The set of associated PublicKeyCredentialSources.
     */
    public List<PublicKeyCredentialSource> getKeysForEntity(@NonNull String rpEntityId) {
        return db.credentialDao().getAllByRpId(rpEntityId);
    }

    /**
     * Get the credential matching the specified id, if it exists
     *
     * @param id byte[] credential id
     * @return PublicKeyCredentialSource that matches the id, or null
     */
    public PublicKeyCredentialSource getCredentialSourceById(@NonNull byte[] id) {
        return db.credentialDao().getById(id);
    }
    public List<PublicKeyCredentialSource> getAllCredentialSource() {
        return db.credentialDao().getAll();
    }

    /**
     * Retrieve a previously-generated keypair from the keystore.
     *
     * @param alias The associated keypair alias.
     * @return A KeyPair object representing the public/private keys. Private key material is
     * not accessible.
     * @throws VirgilException
     */
    public KeyPair getKeyPairByAlias(@NonNull String alias) throws VirgilException {
        KeyStore.Entry keyEntry;
        try {
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, null);
            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
            return new KeyPair(publicKey, privateKey);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
            throw new VirgilException("couldn't get key by alias", e);
        }
    }

    /**
     * Checks whether this key requires user verification or not
     *
     * @param alias The associated keypair alias
     * @return whether this key requires user verification or not
     * @throws VirgilException
     */
    public boolean keyRequiresVerification(@NonNull String alias) throws VirgilException {
        PrivateKey privateKey = getKeyPairByAlias(alias).getPrivate();
        KeyFactory factory;
        KeyInfo keyInfo;

        try {
            factory = KeyFactory.getInstance(privateKey.getAlgorithm(), KEYSTORE_TYPE);
        } catch (NoSuchAlgorithmException | NoSuchProviderException exception) {
            throw new VirgilException("Couldn't build key factory: " + exception.toString());
        }

        try {
            keyInfo = factory.getKeySpec(privateKey, KeyInfo.class);
        } catch (InvalidKeySpecException exception) {
            throw new VirgilException("Not an android keystore key: " + exception.toString());
        }

        return keyInfo.isUserAuthenticationRequired();
    }


    /**
     * Fix the length of a byte array such that:
     * 1) If the desired length is less than the length of `arr`, the left-most source bytes are
     * truncated.
     * 2) If the desired length is more than the length of `arr`, the left-most destination bytes
     * are set to 0x00.
     *
     * @param arr         The source byte array.
     * @param fixedLength The desired length of the resulting array.
     * @return A new array of length fixedLength.
     */
    private static byte[] toUnsignedFixedLength(byte[] arr, int fixedLength) {
        byte[] fixed = new byte[fixedLength];
        int offset = fixedLength - arr.length;
        int srcPos = Math.max(-offset, 0);
        int dstPos = Math.max(offset, 0);
        int copyLength = Math.min(arr.length, fixedLength);
        System.arraycopy(arr, srcPos, fixed, dstPos, copyLength);
        return fixed;
    }

    /**
     * Encode an EC public key in the COSE/CBOR format.
     *
     * @param publicKey The public key.
     * @return A COSE_Key-encoded public key as byte array.
     * @throws VirgilException
     */
    public static byte[] coseEncodePublicKey(PublicKey publicKey) throws VirgilException {
        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        ECPoint point = ecPublicKey.getW();
        // ECPoint coordinates are *unsigned* values that span the range [0, 2**32). The getAffine
        // methods return BigInteger objects, which are signed. toByteArray will output a byte array
        // containing the two's complement representation of the value, outputting only as many
        // bytes as necessary to do so. We want an unsigned byte array of length 32, but when we
        // call toByteArray, we could get:
        // 1) A 33-byte array, if the point's unsigned representation has a high 1 bit.
        //    toByteArray will prepend a zero byte to keep the value positive.
        // 2) A <32-byte array, if the point's unsigned representation has 9 or more high zero
        //    bits.
        // Due to this, we need to either chop off the high zero byte or prepend zero bytes
        // until we have a 32-length byte array.
        byte[] xVariableLength = point.getAffineX().toByteArray();
        byte[] yVariableLength = point.getAffineY().toByteArray();

        byte[] x = toUnsignedFixedLength(xVariableLength, 32);
        assert x.length == 32;
        byte[] y = toUnsignedFixedLength(yVariableLength, 32);
        assert y.length == 32;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put(1, 2)  // kty: EC2 key type
                    .put(3, -7) // alg: ES256 sig algorithm
                    .put(-1, 1) // crv: P-256 curve
                    .put(-2, x) // x-coord
                    .put(-3, y) // y-coord
                    .end()
                    .build()
            );
        } catch (CborException e) {
            throw new VirgilException("couldn't serialize to cbor", e);
        }
        return baos.toByteArray();
    }

    /**
     * Increment the credential use counter for this credential.
     *
     * @param credential The credential whose counter we want to increase.
     * @return The value of the counter before incrementing.
     */
    public int incrementCredentialUseCounter(PublicKeyCredentialSource credential) {
        return db.credentialDao().incrementUseCounter(credential);
    }
}
