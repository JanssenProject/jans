package io.jans.chip.utils.biometric


import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.jans.jans_chip.R
import java.security.Signature
/*
 * BiometricHelper is a utility object that simplifies the implementation of biometric authentication
 * functionalities in Android apps. It provides methods to check biometric availability, register user
 * biometrics, and authenticate users using biometric authentication.
 *
 * This object encapsulates the logic for interacting with the BiometricPrompt API and integrates
 * seamlessly with the CryptoManager to encrypt and decrypt sensitive data for secure storage.
 */
object BiometricHelper {

    // Check if biometric authentication is available on the device
    fun isBiometricAvailable(context: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> {
                Log.e("TAG", "Biometric authentication not available")
                false
            }
        }
    }

    // Retrieve a BiometricPrompt instance with a predefined callback
    private fun getBiometricPrompt(
        context: FragmentActivity,
        onAuthSucceed: (BiometricPrompt.AuthenticationResult) -> Unit
    ): BiometricPrompt {
        val biometricPrompt =
            BiometricPrompt(
                context,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    // Handle successful authentication
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        Log.e("TAG", "Authentication Succeeded: ${result.cryptoObject}")
                        // Execute custom action on successful authentication
                        onAuthSucceed(result)
                    }

                    // Handle authentication errors
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Log.e("TAG", "onAuthenticationError")
                        // TODO: Implement error handling
                    }

                    // Handle authentication failures
                    override fun onAuthenticationFailed() {
                        Log.e("TAG", "onAuthenticationFailed")
                        // TODO: Implement failure handling
                    }
                }
            )
        return biometricPrompt
    }

    // Create BiometricPrompt.PromptInfo with customized display text
    private fun getPromptInfo(
        context: FragmentActivity,
        title: String,
        subTitle: String,
        description: String
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subTitle)
            .setDescription(description)
            .setConfirmationRequired(false)
            .setNegativeButtonText(context.getString(R.string.enable_biometric_dialog_dismiss_btn_text))
            .build()
    }

    // Register user biometrics by encrypting a randomly generated token
    /*fun registerUserBiometrics(
        context: FragmentActivity,
        prefBiometric: String,
        onSuccess: (authResult: BiometricPrompt.AuthenticationResult) -> Unit = {},
    ) {
        val cryptoManager = CryptoManager()
        val cipher = cryptoManager.initEncryptionCipher(SECRET_KEY)
        val biometricPrompt = getBiometricPrompt(context) { authResult ->
            authResult.cryptoObject?.cipher?.let { cipher ->
                // Dummy token for now(in production app, generate a unique and genuine token
                // for each user registration or consider using token received from authentication server)
                val token = UUID.randomUUID().toString()
                val encryptedToken = cryptoManager.encrypt(token, cipher)
                cryptoManager.saveToPrefs(
                    encryptedToken,
                    context,
                    ENCRYPTED_FILE_NAME,
                    Context.MODE_PRIVATE,
                    prefBiometric,
                    *//*PREF_BIOMETRIC*//*
                )
                // Execute custom action on successful registration
                onSuccess(authResult)
            }
        }
        biometricPrompt.authenticate(
            getPromptInfo(
                context,
                "Fido Enrolment",
                "Enrol using your biometric credential",
                "Touch the fingerprint sensor"
            ),
            BiometricPrompt.CryptoObject(cipher)
        )
    }*/
    fun registerUserBiometrics(
        context: FragmentActivity,
        signature: Signature,
        onSuccess: (authResult: BiometricPrompt.AuthenticationResult) -> Unit = {},
    ) {
        //val cryptoManager = CryptoManager()
        //val cipher = cryptoManager.initEncryptionCipher(SECRET_KEY)
        val biometricPrompt = getBiometricPrompt(context) { authResult ->

            onSuccess(authResult)
        }
        biometricPrompt.authenticate(
            getPromptInfo(
                context,
                "Fido Enrolment",
                "Enrol using your biometric credential",
                "Touch the fingerprint sensor"
            ),
            BiometricPrompt.CryptoObject(signature)
        )
    }

    // Authenticate user using biometrics by decrypting stored token
    fun authenticateUser(
        context: FragmentActivity,
        signature: Signature,
        onSuccess: (authResult: BiometricPrompt.AuthenticationResult) -> Unit,
    ) {
        /*val cryptoManager = CryptoManager()
        val encryptedData = cryptoManager.getFromPrefs(
            context,
            ENCRYPTED_FILE_NAME,
            Context.MODE_PRIVATE,
            prefBiometric*/
            /*PREF_BIOMETRIC*/
        //)
        //encryptedData?.let { data ->
        //    val cipher = cryptoManager.initDecryptionCipher(SECRET_KEY, data.initializationVector)
            val biometricPrompt = getBiometricPrompt(context) { authResult ->
                //authResult.cryptoObject?.cipher?.let { cipher ->
                    //val plainText = cryptoManager.decrypt(data.ciphertext, cipher)
                    // Execute custom action on successful authentication

                //}
                onSuccess(authResult)
            }
            val promptInfo = getPromptInfo(
                context,
                "Fido Authentication",
                "Authenticate using your biometric credential",
                "Touch the fingerprint sensor"
            )
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(signature))
        //}
    }
}