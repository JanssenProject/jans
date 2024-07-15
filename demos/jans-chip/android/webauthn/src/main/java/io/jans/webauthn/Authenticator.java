package io.jans.webauthn;

import android.content.Context;
import android.content.DialogInterface;
import android.os.CancellationSignal;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Exchanger;

import io.jans.webauthn.exceptions.ConstraintError;
import io.jans.webauthn.exceptions.InvalidStateError;
import io.jans.webauthn.exceptions.NotAllowedError;
import io.jans.webauthn.exceptions.NotSupportedError;
import io.jans.webauthn.exceptions.VirgilException;
import io.jans.webauthn.exceptions.WebAuthnException;
import io.jans.webauthn.models.AttestationObject;
import io.jans.webauthn.models.AuthenticatorGetAssertionOptions;
import io.jans.webauthn.models.AuthenticatorGetAssertionResult;
import io.jans.webauthn.models.AuthenticatorMakeCredentialOptions;
import io.jans.webauthn.models.NoneAttestationObject;
import io.jans.webauthn.models.PublicKeyCredentialDescriptor;
import io.jans.webauthn.models.PublicKeyCredentialSource;

import io.jans.webauthn.util.CredentialSafe;
import io.jans.webauthn.util.CredentialSelector;
import io.jans.webauthn.util.WebAuthnCryptography;

public class Authenticator {
    private static final String TAG = "WebauthnAuthenticator";
    public static final int SHA_LENGTH = 32;
    public static final int AUTHENTICATOR_DATA_LENGTH = 141;

    private static final Pair<String, Long> ES256_COSE = new Pair<>("public-key", (long) -7);

    public CredentialSafe getCredentialSafe() {
        return credentialSafe;
    }

    CredentialSafe credentialSafe;
    WebAuthnCryptography cryptoProvider;

    /**
     * Construct a WebAuthn authenticator backed by a credential safe and cryptography provider.
     *
     * @param ctx                    Application context for database creation
     * @param authenticationRequired require user authentication via biometrics
     * @param strongboxRequired      require that keys are stored in HSM
     */
    public Authenticator(Context ctx, boolean authenticationRequired, boolean strongboxRequired) throws VirgilException {
        this.credentialSafe = new CredentialSafe(ctx, authenticationRequired, strongboxRequired);
        this.cryptoProvider = new WebAuthnCryptography(this.credentialSafe);
    }


    /**
     * Perform the authenticatorMakeCredential operation as defined by the WebAuthn spec: https://www.w3.org/TR/webauthn/#op-make-cred
     * This will fail if the Authenticator is configured with authentication required
     *
     * @param options The options / arguments to the authenticatorMakeCredential operation.
     * @return an AttestationObject containing the new credential and attestation information
     * @throws VirgilException
     * @throws WebAuthnException
     * @throws VirgilException
     */
    public AttestationObject makeCredential(AuthenticatorMakeCredentialOptions options) throws WebAuthnException, VirgilException {
        if (credentialSafe.supportsUserVerification()) {
            throw new VirgilException("User Verification requires passing a context to makeCredential");
        }
        return makeCredential(options, null, null, null);
    }


    /*public AttestationObject makeCredential(AuthenticatorMakeCredentialOptions options, Context ctx, CancellationSignal cancellationSignal) throws WebAuthnException, VirgilException {
        // We use a flag here rather than explicitly invoking deny-behavior here because the
        // WebAuthn spec asks us to pretend everything is normal for a while (asking user consent)
        // in order to ensure privacy guarantees.
        boolean excludeFlag = false; // whether the excludeCredentialDescriptorList matched one of our credentials

        // 1. Check if all supplied parameters are syntactically well-formed and of the correct length.
        if (!options.areWellFormed()) {
            Log.w(TAG, "Credential Options are not syntactically well-formed.");
            throw new UnknownError();
        }

        // 2. Check if we support a compatible credential type
        if (!options.credTypesAndPubKeyAlgs.contains(ES256_COSE)) {
            Log.w(TAG, "only ES256 is supported");
            throw new NotSupportedError();
        }

        // 3. Check excludeCredentialDescriptorList for existing credentials for this RP
        if (options.excludeCredentialDescriptorList != null) {
            for (PublicKeyCredentialDescriptor descriptor : options.excludeCredentialDescriptorList) {
                // if we already have a credential identified by this id
                PublicKeyCredentialSource existingCredentialSource = this.credentialSafe.getCredentialSourceById(descriptor.id);
                if (existingCredentialSource != null && existingCredentialSource.rpId.equals(options.rpEntity.id) && existingCredentialSource.type.equals(descriptor.type)) {
                    excludeFlag = true;
                }
            }
        }


        // 4. Check requireResidentKey
        // Our authenticator will store resident keys regardless, so we can disregard the value of this parameter

        // 5. Check requireUserVerification
        if (options.requireUserVerification && !this.credentialSafe.supportsUserVerification()) {
            Log.w(TAG, "user verification required but not available");
            throw new ConstraintError();
        }

        // NOTE: We are switching the order of Steps 6 and 7/8 because Android needs to have the credential
        //       created in order to use it in a biometric prompt
        //       We will delete the credential if the biometric prompt fails

        // 7. Generate a new credential
        PublicKeyCredentialSource credentialSource;
        try {
            credentialSource = this.credentialSafe.generateCredential(
                    options.rpEntity.id,
                    options.userEntity.id,
                    options.userEntity.name);
        } catch (VirgilException e) {
            // 8. If any error occurred, return an error code equivalent to "UnknownError"
            Log.w(TAG, "couldn't generate credential", e);
            throw new UnknownError();
        }

        // 6. Obtain user consent for creating a new credential
        // if we need to obtain user verification, create a biometric prompt for that
        // else just generate a new credential/attestation object
        AttestationObject attestationObject = null;
        if (credentialSafe.supportsUserVerification()) {
            if (ctx == null) {
                throw new VirgilException("User Verification requires passing a context to makeCredential");
            }

            // create an Exchanger to retrieve our attestationObject later
            Exchanger<AttestationObject> exchanger = new Exchanger<>();

            // build our biometric callback
            final BiometricPrompt.AuthenticationCallback biometricMakeCredentialCallback =  getAuthenticationMakeCallback(this, options, credentialSource, exchanger);
            BiometricPrompt bp = new BiometricPrompt((FragmentActivity) ctx, ctx.getMainExecutor(), biometricMakeCredentialCallback);

            // build the biometric prompt

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setDescription("Touch the fingerprint sensor")
                    .setTitle("Fido Enrolment")
                    .setSubtitle("Enrol using your biometric credential" )
                    .setNegativeButtonText("Cancel")
                    .build();

            // create our signature object
            PrivateKey privateKey = credentialSafe.getKeyPairByAlias(credentialSource.keyPairAlias).getPrivate();
            Signature signature = WebAuthnCryptography.generateSignatureObject(privateKey);
            BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(signature);

            if (cancellationSignal == null) {
                cancellationSignal = new CancellationSignal();
            }
            try {
                bp.authenticate(promptInfo, cryptoObject);

                attestationObject = exchanger.exchange(null); // pass null to biometric prompt thread
            } catch (Exception *//*| InterruptedException*//* exception) {
                exception.printStackTrace();
                throw new VirgilException("Could not retrieve attestationObject from BiometricPrompt: " + exception.toString());
            }

            if (attestationObject == null) { // null is sent back on failure
                this.credentialSafe.deleteCredential(credentialSource);
                Log.w(TAG, "Biometric authentication failed.");
                throw new NotAllowedError();
            }
        } else {
            // MakeCredentialOptions steps 9 through 13
            attestationObject = makeInternalCredential(options, credentialSource);
        }

        // We finish up step 3 here by checking excludeFlag at the end (so we've still gotten
        // the user's consent to create a credential etc).
        if (excludeFlag) {
            this.credentialSafe.deleteCredential(credentialSource);
            Log.w(TAG, "Credential is excluded by excludeCredentialDescriptorList");
            throw new InvalidStateError();
        }
        return attestationObject;
    }*/
   // 7. Generate a new credential
    public PublicKeyCredentialSource getPublicKeyCredentialSource(AuthenticatorMakeCredentialOptions options) {
        PublicKeyCredentialSource credentialSource;
        try {
            credentialSource = this.credentialSafe.generateCredential(
                    options.rpEntity.id,
                    options.userEntity.id,
                    options.userEntity.name);
            return credentialSource;
        } catch (VirgilException e) {
            // 8. If any error occurred, return an error code equivalent to "UnknownError"
            Log.w(TAG, "couldn't generate credential", e);
            throw new UnknownError();
        }
    }

    /**
     * Generate a signature object to be unlocked via biometric prompt
     * This signature object should be passed down to performSignature
     **/
    public Signature generateSignature(PublicKeyCredentialSource credentialSource) throws VirgilException {
        PrivateKey privateKey = credentialSafe.getKeyPairByAlias(credentialSource.keyPairAlias).getPrivate();
        return WebAuthnCryptography.generateSignatureObject(privateKey);
    }
    /**
     * Perform the authenticatorMakeCredential operation as defined by the WebAuthn spec: https://www.w3.org/TR/webauthn/#op-make-cred
     *
     * @param options The options / arguments to the authenticatorMakeCredential operation.
     * @param ctx     The Main/UI context to be used to display a biometric prompt (if required)
     * @return an AttestationObject containing the new credential and attestation information
     * @throws VirgilException
     * @throws WebAuthnException
     */
    public AttestationObject makeCredential(AuthenticatorMakeCredentialOptions options, PublicKeyCredentialSource credentialSource, Context ctx, CancellationSignal cancellationSignal) throws WebAuthnException, VirgilException {
        // We use a flag here rather than explicitly invoking deny-behavior here because the
        // WebAuthn spec asks us to pretend everything is normal for a while (asking user consent)
        // in order to ensure privacy guarantees.
        boolean excludeFlag = false; // whether the excludeCredentialDescriptorList matched one of our credentials

        // 1. Check if all supplied parameters are syntactically well-formed and of the correct length.
        if (!options.areWellFormed()) {
            Log.w(TAG, "Credential Options are not syntactically well-formed.");
            throw new UnknownError();
        }

        // 2. Check if we support a compatible credential type
        if (!options.credTypesAndPubKeyAlgs.contains(ES256_COSE)) {
            Log.w(TAG, "only ES256 is supported");
            throw new NotSupportedError();
        }

        // 3. Check excludeCredentialDescriptorList for existing credentials for this RP
        if (options.excludeCredentialDescriptorList != null) {
            for (PublicKeyCredentialDescriptor descriptor : options.excludeCredentialDescriptorList) {
                // if we already have a credential identified by this id
                PublicKeyCredentialSource existingCredentialSource = this.credentialSafe.getCredentialSourceById(descriptor.id);
                if (existingCredentialSource != null && existingCredentialSource.rpId.equals(options.rpEntity.id) && existingCredentialSource.type.equals(descriptor.type)) {
                    excludeFlag = true;
                }
            }
        }


        // 4. Check requireResidentKey
        // Our authenticator will store resident keys regardless, so we can disregard the value of this parameter

        // 5. Check requireUserVerification
        if (options.requireUserVerification && !this.credentialSafe.supportsUserVerification()) {
            Log.w(TAG, "user verification required but not available");
            throw new ConstraintError();
        }

        // NOTE: We are switching the order of Steps 6 and 7/8 because Android needs to have the credential
        //       created in order to use it in a biometric prompt
        //       We will delete the credential if the biometric prompt fails

        // 7. Generate a new credential .. shifted to getPublicKeyCredentialSource

        // 6. Obtain user consent for creating a new credential
        // if we need to obtain user verification, create a biometric prompt for that
        // else just generate a new credential/attestation object
        AttestationObject attestationObject = null;
        if (credentialSafe.supportsUserVerification()) {
            if (ctx == null) {
                throw new VirgilException("User Verification requires passing a context to makeCredential");
            }

            // create an Exchanger to retrieve our attestationObject later
            //Exchanger<AttestationObject> exchanger = new Exchanger<>();

            // build our biometric callback
            //final BiometricPrompt.AuthenticationCallback biometricMakeCredentialCallback =  getAuthenticationMakeCallback(this, options, credentialSource, exchanger);
            //BiometricPrompt bp = new BiometricPrompt((FragmentActivity) ctx, ctx.getMainExecutor(), biometricMakeCredentialCallback);

            // build the biometric prompt

            if (cancellationSignal == null) {
                cancellationSignal = new CancellationSignal();
            }
            try {
                //bp.authenticate(promptInfo, cryptoObject);

                //attestationObject = exchanger.exchange(null); // pass null to biometric prompt thread
                attestationObject = makeInternalCredential(options, credentialSource);
            } catch (Exception /*| InterruptedException*/ exception) {
                exception.printStackTrace();
                throw new VirgilException("Could not retrieve attestationObject from BiometricPrompt: " + exception.toString());
            }

            if (attestationObject == null) { // null is sent back on failure
                this.credentialSafe.deleteCredential(credentialSource);
                Log.w(TAG, "Biometric authentication failed.");
                throw new NotAllowedError();
            }
        } else {
            // MakeCredentialOptions steps 9 through 13
            attestationObject = makeInternalCredential(options, credentialSource);
        }

        // We finish up step 3 here by checking excludeFlag at the end (so we've still gotten
        // the user's consent to create a credential etc).
        if (excludeFlag) {
            this.credentialSafe.deleteCredential(credentialSource);
            Log.w(TAG, "Credential is excluded by excludeCredentialDescriptorList");
            throw new InvalidStateError();
        }
        return attestationObject;
    }

    private BiometricPrompt.AuthenticationCallback getAuthenticationMakeCallback(Authenticator authenticator, AuthenticatorMakeCredentialOptions options, PublicKeyCredentialSource credentialSource, Exchanger<AttestationObject> exchanger) {
        // Callback for biometric authentication result
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
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

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
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
        };
    }

    /**
     * Complete steps 9 through 13 of the authenticatorMakeCredential operation as described in the spec:
     * https://www.w3.org/TR/webauthn/#op-make-cred
     *
     * @param options          The options / arguments to the authenticatorMakeCredential operation.
     * @param credentialSource The handle used to lookup the keypair for this credential
     * @return an AttestationObject containing the new credential and attestation information
     * @throws VirgilException
     * @throws WebAuthnException
     */
    public AttestationObject makeInternalCredential(AuthenticatorMakeCredentialOptions options, PublicKeyCredentialSource credentialSource) throws VirgilException, WebAuthnException {
        return makeInternalCredential(options, credentialSource, null);
    }

    /**
     * The second-half of the makeCredential process
     *
     * @param options          The options / arguments to the authenticatorMakeCredential operation.
     * @param credentialSource The handle used to lookup the keypair for this credential
     * @param signature        If not null, use this pre-authorized signature object for the signing operation
     * @return an AttestationObject containing the new credential and attestation information
     * @throws VirgilException
     * @throws WebAuthnException
     */
    public AttestationObject makeInternalCredential(AuthenticatorMakeCredentialOptions options, PublicKeyCredentialSource credentialSource, Signature signature) throws VirgilException, WebAuthnException {

        // TODO: 9. Process extensions
        // Currently not supported

        // 10. Allocate a signature counter for the new credential, initialized at 0
        // It is created and initialized to 0 during creation in step 7

        // 11. Generate attested credential data
        byte[] attestedCredentialData = constructAttestedCredentialData(credentialSource);

        // 12. Create authenticatorData byte array
        byte[] rpIdHash = this.cryptoProvider.sha256(options.rpEntity.id); // 32 bytes
        byte[] authenticatorData = constructAuthenticatorData(rpIdHash, attestedCredentialData, 0); // 141 bytes

        // 13. Return attestation object
        AttestationObject attestationObject = constructAttestationObject(authenticatorData, options.clientDataHash, credentialSource.keyPairAlias, signature);
        return attestationObject;
    }

    /**
     * Perform the authenticatorGetAssertion operation as defined by the WebAuthn spec: https://www.w3.org/TR/webauthn/#op-get-assertion
     *
     * @param options            The options / arguments to the authenticatorGetAssertion operation.
     * @param credentialSelector A CredentialSelector object that can, if needed, prompt the user to select a credential
     * @return a record class containing the output of the authenticatorGetAssertion operation.
     * @throws WebAuthnException
     * @throws VirgilException
     */
    public AuthenticatorGetAssertionResult getAssertion(AuthenticatorGetAssertionOptions options, PublicKeyCredentialSource selectedCredential, CredentialSelector credentialSelector) throws WebAuthnException, VirgilException {
        return getAssertion(options, selectedCredential, credentialSelector, null, null);
    }

    public String urlEncodeToString(byte[] src) {
        return Base64.encodeToString(src, Base64.URL_SAFE);
    }


    public PublicKeyCredentialSource selectPublicKeyCredentialSource(CredentialSelector credentialSelector, AuthenticatorGetAssertionOptions options) throws VirgilException, NotAllowedError {
        List<PublicKeyCredentialSource> credentials = this.credentialSafe.getKeysForEntity(options.rpId);

        // 2-3. Parse allowCredentialDescriptorList
        if (options.allowCredentialDescriptorList != null && options.allowCredentialDescriptorList.size() > 0) {
            List<PublicKeyCredentialSource> filteredCredentials = new ArrayList<>();
            Set<ByteBuffer> allowedCredentialIds = new HashSet<>();
            for (PublicKeyCredentialDescriptor descriptor : options.allowCredentialDescriptorList) {
                allowedCredentialIds.add(ByteBuffer.wrap(descriptor.id));
            }

            for (PublicKeyCredentialSource credential : credentials) {
                if (allowedCredentialIds.contains(ByteBuffer.wrap(credential.id))) {
                    filteredCredentials.add(credential);
                }
            }

            credentials = filteredCredentials;
        }

        // 6. Error if none exist
        if (credentials == null || credentials.size() == 0) {
            Log.i(TAG, "No credentials for this RpId exist");
            throw new NotAllowedError();
        }

        // 7. Allow the user to pick a specific credential, get verification
        PublicKeyCredentialSource selectedCredential;
        if (credentials.size() == 1) {
            selectedCredential = credentials.get(0);
        } else {
            selectedCredential = credentialSelector.selectFrom(credentials);
            if (selectedCredential == null) {
                throw new VirgilException("User did not select credential");
            }
        }
        return selectedCredential;
    }
    /**
     * Perform the authenticatorGetAssertion operation as defined by the WebAuthn spec: https://www.w3.org/TR/webauthn/#op-get-assertion
     *
     * @param options            The options / arguments to the authenticatorGetAssertion operation.
     * @param credentialSelector A CredentialSelector object that can, if needed, prompt the user to select a credential
     * @param ctx                The Main/UI context to be used to display a biometric prompt (if required)
     * @return a record class containing the output of the authenticatorGetAssertion operation.
     * @throws WebAuthnException
     * @throws VirgilException
     */
    public AuthenticatorGetAssertionResult getAssertion(AuthenticatorGetAssertionOptions options, PublicKeyCredentialSource selectedCredential, CredentialSelector credentialSelector, Context ctx, CancellationSignal cancellationSignal) throws WebAuthnException, VirgilException {

        // 1. Check if all supplied parameters are well-formed
        if (!options.areWellFormed()) {
            Log.w(TAG, "GetAssertion Options are not syntactically well-formed.");
            throw new UnknownError();
        }

        // 2-3. Parse allowCredentialDescriptorList
        // we do this slightly out of order, see below.

        // 4-5. Get keys that match this relying party ID


        // get verification, if necessary
        AuthenticatorGetAssertionResult result;
        boolean keyNeedsUnlocking = credentialSafe.keyRequiresVerification(selectedCredential.keyPairAlias);
        if (options.requireUserVerification || keyNeedsUnlocking) {
            if (ctx == null) {
                throw new VirgilException("User Verification requires passing a context to getAssertion");
            }

            // create an Exchanger to retrieve our attestationObject later
            //Exchanger<AuthenticatorGetAssertionResult> exchanger = new Exchanger<>();

            // build our biometric callback
            //final BiometricPrompt.AuthenticationCallback biometricGetAssertionCallback =  getAuthenticationGetCallback(this, options, selectedCredential, exchanger);
            //BiometricPrompt bp = new BiometricPrompt((FragmentActivity) ctx, ctx.getMainExecutor(), biometricGetAssertionCallback);
            //final BiometricGetAssertionCallback biometricGetAssertionCallback = new BiometricGetAssertionCallback(this, options, selectedCredential, exchanger);
            // build the biometric prompt

            /*BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setDescription("Touch the fingerprint sensor")
                    .setTitle("Fido Authentication")
                    .setSubtitle("Authenticate using your biometric credential" )
                    .setNegativeButtonText("Cancel")
                    .build();*/

            // create our signature object
            //PrivateKey privkey = credentialSafe.getKeyPairByAlias(selectedCredential.keyPairAlias).getPrivate();
            //Signature signature = WebAuthnCryptography.generateSignatureObject(privkey);
            //BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(signature);

            if (cancellationSignal == null) {
                cancellationSignal = new CancellationSignal();
            }
            //bp.authenticate(promptInfo, cryptoObject);
            try {
                //result = exchanger.exchange(null); // pass null to biometric prompt thread
                result = getInternalAssertion(options, selectedCredential);
            } catch (Exception exception) {
                Log.w(TAG, "Could not retrieve attestationObject from BiometricPrompt", exception);
                throw new VirgilException("Could not retrieve attestationObject from BiometricPrompt", exception);
            }
            if (result == null) { // failure condition
                Log.w(TAG, "Biometric Authentication failed.");
                throw new NotAllowedError();
            }
        } else { // no biometric
            // steps 8-13
            result = getInternalAssertion(options, selectedCredential);
        }
        return result;
    }

    /**
     * The second half of the getAssertion process
     *
     * @param options            The options / arguments to the authenticatorGetAssertion operation.
     * @param selectedCredential The credential metadata we're using for this assertion
     * @return the credential assertion
     * @throws WebAuthnException
     * @throws VirgilException
     */
    public AuthenticatorGetAssertionResult getInternalAssertion(AuthenticatorGetAssertionOptions options, PublicKeyCredentialSource selectedCredential) throws WebAuthnException, VirgilException {
        return getInternalAssertion(options, selectedCredential, null);
    }

    /**
     * The second half of the getAssertion process
     *
     * @param options            The options / arguments to the authenticatorGetAssertion operation.
     * @param selectedCredential The credential metadata we're using for this assertion
     * @param signature          If not null, use this pre-authorized signature object for the signing operation
     * @return the credential assertion
     * @throws WebAuthnException
     * @throws VirgilException
     */
    public AuthenticatorGetAssertionResult getInternalAssertion(AuthenticatorGetAssertionOptions options, PublicKeyCredentialSource selectedCredential, Signature signature) throws WebAuthnException, VirgilException {

        byte[] authenticatorData;
        byte[] signatureBytes;
        try {
            // TODO 8. Process extensions
            // currently not supported

            // 9. Increment signature counter
            int authCounter = credentialSafe.incrementCredentialUseCounter(selectedCredential);

            // 10. Construct authenticatorData
            byte[] rpIdHash = this.cryptoProvider.sha256(options.rpId); // 32 bytes
            authenticatorData = constructAuthenticatorData(rpIdHash, null, authCounter);

            // 11. Sign the concatentation authenticatorData || hash
            ByteBuffer byteBuffer = ByteBuffer.allocate(authenticatorData.length + options.clientDataHash.length);
            byteBuffer.put(authenticatorData);
            byteBuffer.put(options.clientDataHash);
            byte[] toSign = byteBuffer.array();
            KeyPair keyPair = this.credentialSafe.getKeyPairByAlias(selectedCredential.keyPairAlias);
            signatureBytes = this.cryptoProvider.performSignature(keyPair.getPrivate(), toSign, signature);
            Log.d(TAG, "Performed signature using credential keyPairAlias: " + selectedCredential.keyPairAlias);

            // 12. Throw UnknownError if any error occurs while generating the assertion signature
        } catch (Exception e) {
            Log.w(TAG, "Exception occurred while generating assertion", e);
            throw new UnknownError();
        }

        // 13. Package up the results
        AuthenticatorGetAssertionResult result = new AuthenticatorGetAssertionResult(
                selectedCredential.id,
                authenticatorData,
                signatureBytes,
                selectedCredential.userHandle
        );
        return result;
    }

    /**
     * Construct an attestedCredentialData object per the WebAuthn spec: https://www.w3.org/TR/webauthn/#sec-attested-credential-data
     *
     * @param credentialSource the PublicKeyCredentialSource associated with this credential
     * @return a byte array following the attestedCredentialData format from the WebAuthn spec
     * @throws VirgilException
     */
    private byte[] constructAttestedCredentialData(PublicKeyCredentialSource credentialSource) throws VirgilException {
        // | AAGUID | L | credentialId | credentialPublicKey |
        // |   16   | 2 |      32      |          n          |
        // total size: 50+n
        KeyPair keyPair = this.credentialSafe.getKeyPairByAlias(credentialSource.keyPairAlias);
        byte[] encodedPublicKey = this.credentialSafe.coseEncodePublicKey(keyPair.getPublic());

        ByteBuffer credentialData = ByteBuffer.allocate(16 + 2 + credentialSource.id.length + encodedPublicKey.length);

        // AAGUID will be 16 bytes of zeroes
        credentialData.position(16);
        credentialData.putShort((short) credentialSource.id.length); // L
        credentialData.put(credentialSource.id); // credentialId
        credentialData.put(encodedPublicKey);
        return credentialData.array();
    }

    /**
     * Construct an authenticatorData object per the WebAuthn spec: https://www.w3.org/TR/webauthn/#sec-authenticator-data
     *
     * @param rpIdHash               the SHA-256 hash of the rpId
     * @param attestedCredentialData byte array containing the attested credential data
     * @return a byte array that matches the authenticatorData format
     * @throws VirgilException
     */
    private byte[] constructAuthenticatorData(byte[] rpIdHash, byte[] attestedCredentialData, int authCounter) throws VirgilException {
        if (rpIdHash.length != 32) {
            throw new VirgilException("rpIdHash must be a 32-byte SHA-256 hash");
        }

        byte flags = 0x00;
        flags |= 0x01; // user present
        if (this.credentialSafe.supportsUserVerification()) {
            flags |= (0x01 << 2); // user verified
        }
        if (attestedCredentialData != null) {
            flags |= (0x01 << 6); // attested credential data included
        }

        // 32-byte hash + 1-byte flags + 4 bytes signCount = 37 bytes
        ByteBuffer authData = ByteBuffer.allocate(37 +
                (attestedCredentialData == null ? 0 : attestedCredentialData.length));

        authData.put(rpIdHash);
        authData.put(flags);
        authData.putInt(authCounter);
        if (attestedCredentialData != null) {
            authData.put(attestedCredentialData);
        }
        return authData.array();
    }

    /**
     * Construct an AttestationObject per the WebAuthn spec: https://www.w3.org/TR/webauthn/#generating-an-attestation-object
     * We use either packed self-attestation or "none" attestation: https://www.w3.org/TR/webauthn/#attestation-formats
     * The signing procedure is documented here under `Signing Procedure`->4. : https://www.w3.org/TR/webauthn/#packed-attestation
     *
     * @param authenticatorData byte array containing the raw authenticatorData object
     * @param clientDataHash    byte array containing the sha256 hash of the client data object (request type, challenge, origin)
     * @param keyPairAlias      alias to lookup the key pair to be used to sign the attestation object
     * @return a well-formed AttestationObject structure
     * @throws VirgilException
     */
    private AttestationObject constructAttestationObject(byte[] authenticatorData, byte[] clientDataHash, String keyPairAlias, Signature signature) throws VirgilException {
        // Our goal in this function is primarily to create a signature over the relevant data fields
        // From https://www.w3.org/TR/webauthn/#packed-attestation we can see that for self-signed attestation,
        // `sig` is generated by signing the concatenation of authenticatorData and clientDataHash
        // Once we have constructed `sig`, we create a new AttestationObject to contain the
        // authenticatorData and `sig`.
        // The AttestationObject has a .asCBOR() method that will properly construct the full,
        // encoded attestation object in a format that can be returned to the client/relying party
        // (shown in Figure 5 of the webauthn spec)

        // Concatenate authenticatorData so we can sign them.
        // "If self attestation is in use, the authenticator produces sig by concatenating
        // authenticatorData and clientDataHash, and signing the result using the credential
        // private key."
        ByteBuffer byteBuffer = ByteBuffer.allocate(clientDataHash.length + authenticatorData.length);
        byteBuffer.put(authenticatorData);
        byteBuffer.put(clientDataHash);
        byte[] toSign = byteBuffer.array();

        // for testing purposes during development, make a sanity check that the authenticatorData and clientDataHash are the fixed lengths we expect
        //assert toSign.length == 141 + 32;

        // grab our keypair for this credential
        KeyPair keyPair = this.credentialSafe.getKeyPairByAlias(keyPairAlias);
        byte[] signatureBytes = this.cryptoProvider.performSignature(keyPair.getPrivate(), toSign, signature);

        // construct our attestation object (attestationObject.asCBOR() can be used to generate the raw object in calling function)
        // AttestationObject attestationObject = new PackedSelfAttestationObject(authenticatorData, signatureBytes);
        // TODO: Discuss tradeoffs wrt none / packed attestation formats. Switching to none here because packed lacks support.
        AttestationObject attestationObject = new NoneAttestationObject(authenticatorData);
        return attestationObject;
    }


}
