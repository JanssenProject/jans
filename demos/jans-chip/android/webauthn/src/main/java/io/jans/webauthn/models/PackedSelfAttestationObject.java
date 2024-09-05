package io.jans.webauthn.models;

import java.io.ByteArrayOutputStream;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import io.jans.webauthn.exceptions.VirgilException;

public class PackedSelfAttestationObject extends AttestationObject {
    private byte[] signature;

    /**
     * Construct a new self-attestation attObj in packed format.
     *
     * @param authData  The authenticator data signed.
     * @param signature The signature over the concatenation of authenticatorData and
     *                  clientDataHash.
     */
    public PackedSelfAttestationObject(byte[] authData, byte[] signature) {
        this.authData = authData;
        this.signature = signature;
    }

    /**
     * Encode this self-attestation attObj as the CBOR required by the WebAuthn spec
     * https://www.w3.org/TR/webauthn/#sctn-attestation
     * https://www.w3.org/TR/webauthn/#packed-attestation
     *
     * @return CBOR encoding of the attestation object as a byte array
     * @throws VirgilException
     */
    @Override
    public byte[] asCBOR() throws VirgilException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put("authData", this.authData)
                    .put("fmt", "packed")
                    .putMap("attStmt")
                    .put("alg", (long) -7)
                    .put("sig", this.signature)
                    .end()
                    .end()
                    .build()
            );
        } catch (CborException e) {
            throw new VirgilException("couldn't serialize to cbor", e);
        }
        return baos.toByteArray();
    }
}
