package io.jans.webauthn.util;

import android.hardware.biometrics.BiometricPrompt;
import android.util.Log;

import java.security.Signature;
import java.util.concurrent.Exchanger;

import io.jans.webauthn.Authenticator;
import io.jans.webauthn.exceptions.VirgilException;
import io.jans.webauthn.exceptions.WebAuthnException;
import io.jans.webauthn.models.AuthenticatorGetAssertionOptions;
import io.jans.webauthn.models.AuthenticatorGetAssertionResult;
import io.jans.webauthn.models.PublicKeyCredentialSource;

public class BiometricGetAssertionCallback extends BiometricPrompt.AuthenticationCallback {
    private static final String TAG = "BiometricGetAssertionCallback";

    private Authenticator authenticator;
    private AuthenticatorGetAssertionOptions options;
    private PublicKeyCredentialSource selectedCredential;
    private Exchanger<AuthenticatorGetAssertionResult> exchanger;

    public BiometricGetAssertionCallback(Authenticator authenticator, AuthenticatorGetAssertionOptions options, PublicKeyCredentialSource selectedCredential, Exchanger<AuthenticatorGetAssertionResult> exchanger) {
        super();
        this.authenticator = authenticator;
        this.options = options;
        this.selectedCredential = selectedCredential;
        this.exchanger = exchanger;
    }

    @Override
    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        Log.d(TAG, "Authentication Succeeded");

        // retrieve biometricprompt-approved signature
        Signature signature = result.getCryptoObject().getSignature();

        AuthenticatorGetAssertionResult assertionResult;
        try {
            assertionResult = authenticator.getInternalAssertion(options, selectedCredential, signature);
        } catch (VirgilException | WebAuthnException exception) {
            Log.w(TAG, "Failed getInternalAssertion: " + exception.toString());
            onAuthenticationFailed();
            return;
        }
        try {
            exchanger.exchange(assertionResult);
        } catch (InterruptedException exception) {
            Log.w(TAG, "Could not send assertionResult from BiometricPrompt: " + exception.toString());
            return;
        }
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        super.onAuthenticationHelp(helpCode, helpString);
        Log.d(TAG, "authentication help");
    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        Log.d(TAG, "authentication error");
        try {
            exchanger.exchange(null);
        } catch (InterruptedException exception) {
            Log.w(TAG, "Could not send null (failed) from BiometricPrompt: " + exception.toString());
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
            Log.w(TAG, "Could not send null (failed) from BiometricPrompt: " + exception.toString());
        }
    }
}
