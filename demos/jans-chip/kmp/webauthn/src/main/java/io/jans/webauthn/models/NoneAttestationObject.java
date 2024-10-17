package io.jans.webauthn.models;

import android.util.Log;

import java.io.ByteArrayOutputStream;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

public class NoneAttestationObject extends AttestationObject {
    /**
     * Construct a new self-attestation attObj in packed format.
     *
     * @param authData The authenticator data.
     */
    public NoneAttestationObject(byte[] authData) {
        this.authData = authData;
    }

    /**
     * Encode this self-attestation attObj as the CBOR required by the WebAuthn spec
     * https://www.w3.org/TR/webauthn/#sctn-attestation
     * https://www.w3.org/TR/webauthn/#none-attestation
     *
     * @return CBOR encoding of the attestation object as a byte array
     */
    @Override
    public byte[] asCBOR() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put("authData", this.authData)
                    .put("fmt", "none")
                    .putMap("attStmt")
                    .end()
                    .end()
                    .build()
            );
        } catch (CborException e) {
            Log.d("NoneAttestationObject", "couldn't serialize to cbor", e);
        }
        return baos.toByteArray();
    }
}
