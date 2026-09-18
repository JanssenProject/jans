/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.model.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Config-API does not map relying-party fields individually - {@code Fido2ConfigResource} returns and
 * accepts the whole {@code AppConfiguration} - so the REST contract for a per-RP policy is whatever Jackson
 * does with this class. These tests pin that, because nothing else in the build would notice it breaking.
 */
class RequestedPartySerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * The Config-API serialises its responses with {@code NON_EMPTY} inclusion - see
     * {@code ObjectMapperContextResolver} in {@code jans-config-api/shared} - so what a client actually
     * receives is not what a default mapper produces. This module cannot depend on the Config-API to
     * borrow that resolver, so the one setting that decides whether an unset field reaches the wire is
     * mirrored here.
     */
    private final ObjectMapper apiMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private RequestedParty withPolicy(String attestationMode) {
        RequestedParty requestedParty = new RequestedParty();
        requestedParty.setId("example.com");
        requestedParty.setOrigins(Arrays.asList("https://login.example.com"));

        RequestedPartyPolicy policy = new RequestedPartyPolicy();
        policy.setAttestationMode(attestationMode);
        requestedParty.setPolicy(policy);

        return requestedParty;
    }

    @Test
    void policySurvivesARoundTrip() throws Exception {
        String json = mapper.writeValueAsString(withPolicy(AttestationMode.ENFORCED.getValue()));

        RequestedParty parsed = mapper.readValue(json, RequestedParty.class);

        assertEquals("example.com", parsed.getId());
        assertEquals(Arrays.asList("https://login.example.com"), parsed.getOrigins());
        assertEquals(AttestationMode.ENFORCED.getValue(), parsed.getPolicy().getAttestationMode());
    }

    /**
     * A relying party configured before this field existed must still parse, with no policy rather than an
     * empty one - the resolver treats null as "not set" and falls back to the global value.
     */
    @Test
    void aRelyingPartyWithoutAPolicyStillParses() throws Exception {
        String legacyJson = "{\"id\":\"example.com\",\"origins\":[\"https://login.example.com\"]}";

        RequestedParty parsed = mapper.readValue(legacyJson, RequestedParty.class);

        assertEquals("example.com", parsed.getId());
        assertNull(parsed.getPolicy());
    }

    /**
     * Existing API clients must see exactly what they saw before, so a relying party with no policy must
     * not start emitting a policy key.
     */
    @Test
    void aRelyingPartyWithoutAPolicySerialisesAsItDidBefore() throws Exception {
        RequestedParty requestedParty = new RequestedParty();
        requestedParty.setId("example.com");
        requestedParty.setOrigins(Arrays.asList("https://login.example.com"));

        String json = apiMapper.writeValueAsString(requestedParty);

        assertTrue(json.contains("\"id\""), json);
        assertTrue(json.contains("\"origins\""), json);
        assertFalse(json.contains("\"policy\""), json);
    }

    /**
     * The omission above would also hold if the policy never reached the wire at all, so this pins the
     * other half: under the same inclusion rule, a policy that is set is still published.
     */
    @Test
    void aPolicyThatIsSetIsStillSerialised() throws Exception {
        String json = apiMapper.writeValueAsString(withPolicy(AttestationMode.ENFORCED.getValue()));

        assertTrue(json.contains("\"policy\""), json);
        assertTrue(json.contains("\"attestationMode\":\"enforced\""), json);
    }

    /** An unknown field must not break parsing, as {@code @JsonIgnoreProperties} on both types intends. */
    @Test
    void unknownFieldsAreIgnoredOnBothTypes() throws Exception {
        String json = "{\"id\":\"example.com\",\"origins\":[],\"somethingNew\":1,"
                + "\"policy\":{\"attestationMode\":\"monitor\",\"aFutureField\":true}}";

        RequestedParty parsed = mapper.readValue(json, RequestedParty.class);

        assertEquals(AttestationMode.MONITOR.getValue(), parsed.getPolicy().getAttestationMode());
    }

    @Test
    void anEmptyPolicyObjectParsesWithTheFieldUnset() throws Exception {
        String json = "{\"id\":\"example.com\",\"origins\":[],\"policy\":{}}";

        RequestedParty parsed = mapper.readValue(json, RequestedParty.class);

        assertNull(parsed.getPolicy().getAttestationMode());
    }

    /** The policy must not leak into the string form used in debug logs without its value. */
    @Test
    void policyToStringNamesItsField() {
        assertTrue(withPolicy("enforced").getPolicy().toString().contains("enforced"));
        assertFalse(new RequestedPartyPolicy().toString().contains("enforced"));
    }
}
