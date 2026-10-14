// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/**
 * A decision record signed with the RFC 8032 TEST 1 key. Expected values were computed
 * independently with Python {@code json.dumps(sort_keys=True)} (JCS for strings and small
 * integers) and {@code cryptography} Ed25519, not with this library. Test-only.
 */
final class Records {

    static final String KID = "key-2026-01";
    static final String PRODUCER = "cedarling-fleet-1";
    static final String RECORD_ID = "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41";

    /** Unsigned, unordered and pretty-printed: the core canonicalizes before signing. */
    static final String UNSIGNED = "{\n"
            + "  \"record_id\": \"" + RECORD_ID + "\",\n"
            + "  \"producer\": \"" + PRODUCER + "\",\n"
            + "  \"kid\": \"" + KID + "\",\n"
            + "  \"producer_chain\": {\"sequence_number\": 1, \"prev_record_hash\": \"sha256:"
            + "0000000000000000000000000000000000000000000000000000000000000000\"},\n"
            + "  \"trace\": {\"event_kind\": \"AUTHORIZATION_DECISION\", \"note\": \"ї€\"}\n"
            + "}";

    static final String SIGNING_INPUT = "{\"kid\":\"key-2026-01\",\"producer\":\"cedarling-fleet-1\","
            + "\"producer_chain\":{\"prev_record_hash\":\"sha256:"
            + "0000000000000000000000000000000000000000000000000000000000000000\",\"sequence_number\":1},"
            + "\"record_id\":\"9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41\","
            + "\"trace\":{\"event_kind\":\"AUTHORIZATION_DECISION\",\"note\":\"ї€\"}}";

    static final String SIGNATURE =
            "K_yWeo5mlG5HuY1jLDQvCWDT4aJ_oYscoGsiXwaCo334-W-y6KIK3oX6PQfzqRt66kflZHY-rc-Y2ZzPYIMCBw";

    /** JCS of the whole record, signature included: what {@code sign} produces. */
    static final String SIGNED = "{\"kid\":\"key-2026-01\",\"producer\":\"cedarling-fleet-1\","
            + "\"producer_chain\":{\"prev_record_hash\":\"sha256:"
            + "0000000000000000000000000000000000000000000000000000000000000000\",\"sequence_number\":1},"
            + "\"record_id\":\"9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41\","
            + "\"signature\":\"" + SIGNATURE + "\","
            + "\"trace\":{\"event_kind\":\"AUTHORIZATION_DECISION\",\"note\":\"ї€\"}}";

    static final String CONTENT_DIGEST =
            "sha256:0a7f0451ae373077d0e8517eac57e0a8044faf0a22a4205afb53f48ce227dc9d";
    static final String SIGNING_INPUT_SHA256 =
            "sha256:c4a7b4cba579b55fc77223206f070199f4b9567ca1e3a142718dfa7bd8d41f65";

    private Records() {
    }

    static Ed25519Signer signer() throws PrivateKeyException {
        return Ed25519Signer.fromSecretBytes(KID, Rfc8032.SECRET);
    }

    static PublicKey publicKey() throws PublicKeyException {
        return PublicKey.fromBytes(Rfc8032.PUBLIC);
    }

    static SignedAssertion signed() throws TraceException {
        return SignedAssertion.parse(new JsonBody(SIGNED), new Limits());
    }

    /** {@link #SIGNED} with {@code from} replaced by {@code to}. */
    static JsonBody signedWith(String from, String to) {
        if (!SIGNED.contains(from)) {
            throw new IllegalArgumentException(from + " is not in the record");
        }
        return new JsonBody(SIGNED.replace(from, to));
    }
}
