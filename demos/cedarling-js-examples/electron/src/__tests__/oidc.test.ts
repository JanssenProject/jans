import { assertUserinfoSubject, remoteJwks } from "../main/oidc";

test("rejects a signed UserInfo token for a different subject", async () => {
  expect(() => assertUserinfoSubject({ sub: "alice" }, "bob")).toThrow(/subject/);
});

test("rejects a remote plaintext JWKS endpoint", async () => {
  await expect(remoteJwks("http://idp.example/jwks")).rejects.toThrow(/HTTPS/);
});
