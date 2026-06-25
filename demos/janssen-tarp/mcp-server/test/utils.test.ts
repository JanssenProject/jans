import { describe, it } from "node:test";
import assert from "node:assert/strict";
import crypto from "crypto";
import Utils from "../src/utils";

describe("Utils.base64url", () => {
  it("encodes a buffer using URL-safe base64 without padding", () => {
    // 0xFB 0xFF -> standard base64 "+/8=" which must become "-_8"
    const buffer = Buffer.from([0xfb, 0xff]);
    const result = Utils.base64url(buffer);

    assert.equal(result, "-_8");
    assert.ok(!result.includes("+"), "must not contain '+'");
    assert.ok(!result.includes("/"), "must not contain '/'");
    assert.ok(!result.includes("="), "must not contain padding '='");
  });

  it("encodes an empty buffer as an empty string", () => {
    assert.equal(Utils.base64url(Buffer.alloc(0)), "");
  });

  it("is reversible back to the original bytes", () => {
    const original = crypto.randomBytes(32);
    const encoded = Utils.base64url(original);
    const decoded = Buffer.from(encoded, "base64url");

    assert.deepEqual(decoded, original);
  });
});

describe("Utils.codeChallengeFromVerifier", () => {
  it("derives the PKCE S256 challenge from a verifier", () => {
    const verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    // Reference value per RFC 7636 Appendix B.
    const expected = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    assert.equal(Utils.codeChallengeFromVerifier(verifier), expected);
  });

  it("produces a 43-char URL-safe challenge for any verifier", () => {
    const challenge = Utils.codeChallengeFromVerifier("some-random-verifier-value");

    // SHA-256 (32 bytes) base64url-encoded without padding is always 43 chars.
    assert.equal(challenge.length, 43);
    assert.match(challenge, /^[A-Za-z0-9_-]+$/);
  });

  it("is deterministic for the same verifier", () => {
    const v = "abc123";
    assert.equal(
      Utils.codeChallengeFromVerifier(v),
      Utils.codeChallengeFromVerifier(v)
    );
  });
});
