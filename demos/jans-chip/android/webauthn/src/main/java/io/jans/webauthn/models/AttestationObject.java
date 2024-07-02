package io.jans.webauthn.models;

import android.util.Base64;

import java.util.Arrays;

import io.jans.webauthn.exceptions.VirgilException;

public abstract class AttestationObject {
    byte[] authData;

    public abstract byte[] asCBOR() throws VirgilException;

    /**
     * Retrieves the credential_id field from the attestation object and converts it to a string
     * Figure 5 is helpful: https://www.w3.org/TR/webauthn/#attestation-object
     *
     * @return String credential id
     */
    public byte[] getCredentialId() {
        // The credential ID is stored within the attested credential data section of the attestation object
        // field lengths are as follows (in bytes):
        // rpId = 32, flags = 1, counter = 4, AAGUID = 16, L = 2, credential ID = L

        // first we retrieve L, which is at offset 53 (and is big-endian)
        int l = (this.authData[53] << 8) + this.authData[54];
        // then retrieve the credential id field from offset 55
        byte[] credentialId = Arrays.copyOfRange(this.authData, 55, 55 + l);
        return credentialId;
    }

    /**
     * Retrieves the credential_id field from the attestation object and converts it to a string
     * Figure 5 is helpful: https://www.w3.org/TR/webauthn/#attestation-object
     *
     * @return String credential id
     */
    public String getCredentialIdBase64() {
        return Base64.encodeToString(this.getCredentialId(), Base64.NO_WRAP);
    }
}
