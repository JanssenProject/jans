// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;
import io.jans.trace.uniffi.ErrorCode;
import io.jans.trace.TraceException.CanonicalizeException;

public class JsonTest {

    @Test
    public void canonicalizesPerRfc8785() throws CanonicalizeException {
        assertEquals(Json.canonicalize(new JsonBody("{ \"b\": 1.0, \"a\": \"x\" }"), new Limits()),
                "{\"a\":\"x\",\"b\":1}", "members should be sorted and numbers in JCS form");
    }

    @Test
    public void keepsNonAsciiTextExact() throws CanonicalizeException {
        assertEquals(Json.canonicalize(new JsonBody("{\"k\":\"€ї\"}"), new Limits()),
                "{\"k\":\"€ї\"}", "non-ASCII text should cross the boundary unchanged");
    }

    private static void assertRejected(String json, Limits limits, ErrorCode code) {
        CanonicalizeException e = expectThrows(CanonicalizeException.class,
                () -> Json.canonicalize(new JsonBody(json), limits));
        assertEquals(e.code(), code, json + " should be rejected as " + code);
        assertTrue(e instanceof TraceException, "every binding error should be a TraceException");
    }

    @Test
    public void brokenJsonIsInvalidJson() {
        assertRejected("{", new Limits(), ErrorCode.INVALID_JSON);
    }

    @Test
    public void duplicateMemberIsReported() {
        assertRejected("{\"a\":1,\"a\":2}", new Limits(), ErrorCode.DUPLICATE_MEMBER);
    }

    @Test
    public void unsafeIntegerIsNonCanonicalizable() {
        assertRejected("9007199254740992", new Limits(), ErrorCode.NON_CANONICALIZABLE_JSON);
    }

    @Test
    public void oversizedInputExceedsSizeLimit() {
        assertRejected("[1,2]", new Limits().withMaxInputBytes(2), ErrorCode.SIZE_LIMIT_EXCEEDED);
    }

    @Test
    public void tooDeepInputExceedsSizeLimit() {
        assertRejected("[[1]]", new Limits().withMaxDepth(1), ErrorCode.SIZE_LIMIT_EXCEEDED);
    }

    @Test
    public void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> new JsonBody(null));
        assertThrows(NullPointerException.class, () -> Json.canonicalize(null, new Limits()));
        assertThrows(NullPointerException.class, () -> Json.canonicalize(new JsonBody("{}"), null));
    }
}
