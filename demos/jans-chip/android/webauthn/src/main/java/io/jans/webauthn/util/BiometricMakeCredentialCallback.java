package io.jans.webauthn.util;


import android.util.Log;

import androidx.biometric.BiometricPrompt;

import java.security.Signature;
import java.util.concurrent.Exchanger;

import io.jans.webauthn.Authenticator;
import io.jans.webauthn.exceptions.VirgilException;
import io.jans.webauthn.exceptions.WebAuthnException;
import io.jans.webauthn.models.AttestationObject;
import io.jans.webauthn.models.AuthenticatorMakeCredentialOptions;
import io.jans.webauthn.models.PublicKeyCredentialSource;

public class BiometricMakeCredentialCallback extends BiometricPrompt.AuthenticationCallback {
    private static final String TAG = "BiometricMakeCredentialCallback";

    private Authenticator authenticator;
    private AuthenticatorMakeCredentialOptions options;
    private PublicKeyCredentialSource credentialSource;
    private Exchanger<AttestationObject> exchanger;

    public BiometricMakeCredentialCallback(Authenticator authenticator, AuthenticatorMakeCredentialOptions options, PublicKeyCredentialSource credentialSource, Exchanger<AttestationObject> exchanger) {
        super();
        this.authenticator = authenticator;
        this.options = options;
        this.credentialSource = credentialSource;
        this.exchanger = exchanger;
    }

    @Override
    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
        Log.d(TAG, "Authentication Succeeded");
        super.onAuthenticationSucceeded(result);
        Log.d(TAG, "Authentication Succeeded");

        // retrieve biometricprompt-approved signature
        Signature signature = result.getCryptoObject().getSignature();

        AttestationObject attestationObject;
        try {
            attestationObject = authenticator.makeInternalCredential(options, credentialSource, signature);
        } catch (VirgilException | WebAuthnException exception) {
            Log.w(TAG, "Failed makeInternalCredential: " + exception.toString());
            onAuthenticationFailed();
            return;
        }
        try {
            exchanger.exchange(attestationObject);
        } catch (InterruptedException exception) {
            Log.w(TAG, "Could not send attestationObject from BiometricPrompt: " + exception.toString());
            return;
        }
    }

   /* @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        super.onAuthenticationHelp(helpCode, helpString);
        Log.d(TAG, "authentication help");
    }*/

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        Log.d(TAG, "authentication error");
        try {
            exchanger.exchange(null);
        } catch (InterruptedException exception) {
            Log.w(TAG, "Could not send null (failure) from BiometricPrompt: " + exception.toString());
        }
    }

    @Override
    public void onAuthenticationFailed() {
        // this happens on a bad fingerprint read -- don't cancel/error if this happens
        super.onAuthenticationFailed();
        Log.d(TAG, "authentication failed");
    }

    public void onAuthenticationCancelled() {
        Log.d(TAG, "authentication cancelled");
        try {
            exchanger.exchange(null);
        } catch (InterruptedException exception) {
            Log.w(TAG, "Could not send null (failure) from BiometricPrompt: " + exception.toString());
        }
    }
}
