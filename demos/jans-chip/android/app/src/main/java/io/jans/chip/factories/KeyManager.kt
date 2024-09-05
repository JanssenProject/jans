package io.jans.chip.factories

import android.icu.util.Calendar
import android.icu.util.GregorianCalendar
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.RSAKey
import java.io.IOException
import java.math.BigInteger
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.security.interfaces.RSAPublicKey
import javax.security.auth.x500.X500Principal

class KeyManager {



    companion object {
        private val KEY_ALIAS = "JansChipKeystore"
        private val ANDROID_KEYSTORE = "AndroidKeyStore"

        /**
         * The function retrieves the public key from the Android Keystore, generating a new key pair if necessary.
         *
         * @return The method is returning a PublicKey object.
         */
        fun getPublicKey(): PublicKey? {
            return try {
                if (!checkKeyExists()) {
                    generateKeyPair()
                }
                val keyStore =
                    KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                val privateKey = keyStore.getKey(
                    KEY_ALIAS,
                    null
                ) as PrivateKey
                keyStore.getCertificate(KEY_ALIAS).publicKey
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: UnrecoverableKeyException) {
                throw RuntimeException(e)
            } catch (e: CertificateException) {
                throw RuntimeException(e)
            } catch (e: KeyStoreException) {
                throw RuntimeException(e)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        /**
         * The function retrieves the private key from the Android Keystore, generating a new key pair if it doesn't exist.
         *
         * @return The method is returning a PrivateKey object.
         */
        fun getPrivateKey(): PrivateKey? {
            return try {
                if (!checkKeyExists()) {
                    generateKeyPair()
                }
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                val privateKey =
                    keyStore.getKey(KEY_ALIAS, null) as PrivateKey
                val publicKey =
                    keyStore.getCertificate(KEY_ALIAS).publicKey
                privateKey
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: UnrecoverableKeyException) {
                throw RuntimeException(e)
            } catch (e: CertificateException) {
                throw RuntimeException(e)
            } catch (e: KeyStoreException) {
                throw RuntimeException(e)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        /**
         * The function generates a key pair using RSA algorithm with specified parameters and returns the public key.
         *
         * @return The method is returning a PublicKey object.
         */
        private fun generateKeyPair(): PublicKey? {
            try {
                val startDate = GregorianCalendar()
                val endDate = GregorianCalendar()
                endDate.add(Calendar.YEAR, 1)
                val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE
                )
                kpg.initialize(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                    )
                        .setCertificateSerialNumber(BigInteger.valueOf(777)) //Serial number used for the self-signed certificate of the generated key pair, default is 1
                        .setCertificateSubject(X500Principal("CN=" + KEY_ALIAS)) //Subject used for the self-signed certificate of the generated key pair, default is CN=fake
                        .setDigests(KeyProperties.DIGEST_SHA256) //Set of digests algorithms with which the key can be used
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1) //Set of padding schemes with which the key can be used when signing/verifying
                        .setCertificateNotBefore(startDate.time) //Start of the validity period for the self-signed certificate of the generated, default Jan 1 1970
                        .setCertificateNotAfter(endDate.time) //End of the validity period for the self-signed certificate of the generated key, default Jan 1 2048
                        //.setUserAuthenticationRequired(true)                             //Sets whether this key is authorized to be used only if the user has been authenticated, default false
                        .setKeySize(2048)
                        .setUserAuthenticationValidityDurationSeconds(30)
                        .build()
                )
                var kp: KeyPair = kpg.generateKeyPair()
                Log.d("Key pair successfully generated:: Public Key", kp.getPublic().toString())
                Log.d(
                    "Key pair successfully generated:: Public Key",
                    Base64.encodeToString(kp.getPublic().encoded, Base64.NO_WRAP)
                )
                return kp.getPublic()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * The function checks if a private and public key pair exists in the Android Keystore.
         *
         * @return The method is returning a boolean value. It returns true if both the private key and public key exist in the
         * keystore, and false otherwise.
         */
        private fun checkKeyExists(): Boolean {
            //We get the Keystore instance
            var keyStore: KeyStore? = null
            return try {
                keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                //We get the private and public key from the keystore if they exists
                val privateKey =
                    keyStore.getKey(KEY_ALIAS, null) as PrivateKey
                val publicKey =
                    keyStore.getCertificate(KEY_ALIAS).publicKey
                privateKey != null && publicKey != null
            } catch (e: KeyStoreException) {
                false
                //throw new RuntimeException(e);
            } catch (e: UnrecoverableKeyException) {
                false
            } catch (e: CertificateException) {
                false
            } catch (e: IOException) {
                false
            } catch (e: NoSuchAlgorithmException) {
                false
            } catch (e: NullPointerException) {
                false
            }
        }

        /**
         * The function generates a signature for the given data using a private key stored in the Android Keystore.
         *
         * @param data The "data" parameter is a byte array that represents the data that you want to sign.
         * @return The method is returning a String representation of the signature generated from the provided data.
         */
        fun signData(data: ByteArray?): String? {
            var keyStore: KeyStore? = null
            return try {
                if (!checkKeyExists()) {
                    generateKeyPair()
                }
                keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                //We get the private and public key from the keystore if they exists
                val privateKey =
                    keyStore.getKey(KEY_ALIAS, null) as PrivateKey
                //PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
                val s = Signature.getInstance("SHA256withRSA")
                s.initSign(privateKey)
                s.update(data)
                val signature = s.sign().toString(Charsets.UTF_8)
                return signature
            } catch (e: KeyStoreException) {
                throw RuntimeException(e)
            } catch (e: UnrecoverableKeyException) {
                throw RuntimeException(e)
            } catch (e: CertificateException) {
                throw RuntimeException(e)
            } catch (e: IOException) {
                throw RuntimeException(e)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: SignatureException) {
                throw RuntimeException(e)
            } catch (e: InvalidKeyException) {
                throw RuntimeException(e)
            }
        }

        /**
         * The function takes a public key and returns a JWK (JSON Web Key) representation of the key.
         *
         * @param publicKey The publicKey parameter is of type PublicKey and represents the public key that you want to convert
         * to a JWK (JSON Web Key) format.
         * @return The method is returning a JWK (JSON Web Key) object.
         */
        fun getPublicKeyJWK(publicKey: PublicKey?): JWK? {
            return RSAKey.Builder(publicKey as RSAPublicKey?)
                .build()
        }
    }
}