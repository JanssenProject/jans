import {
  rendererContentSecurityPolicy,
  resolveRendererAssetPath,
  withRendererContentSecurityPolicy,
} from "../main/security";

describe("renderer Content Security Policy", () => {
  it("allows only self and the exact external IdP origin for connections", () => {
    const policy = rendererContentSecurityPolicy("https://idp.example");

    expect(policy).toContain("style-src 'self'");
    expect(policy).toContain("connect-src 'self' https://idp.example");
    expect(policy).not.toContain("'unsafe-inline'");
    expect(policy).not.toContain("connect-src *");
  });

  it("rejects a non-origin value before it can become a CSP source", () => {
    expect(() =>
      rendererContentSecurityPolicy("https://idp.example/path"),
    ).toThrow(/exact HTTP\(S\) issuer origin/);
  });

  it("allows Vite injection only in the explicit development policy", () => {
    const policy = rendererContentSecurityPolicy("http://localhost:9090", {
      development: true,
    });

    expect(policy).toContain("script-src 'self' 'wasm-unsafe-eval' 'unsafe-inline'");
    expect(policy).toContain("style-src 'self' 'unsafe-inline'");
    expect(rendererContentSecurityPolicy("http://localhost:9090")).not.toContain(
      "'unsafe-inline'",
    );
  });

  it("replaces an existing CSP header without changing other headers", () => {
    expect(
      withRendererContentSecurityPolicy(
        {
          "content-security-policy": ["default-src *"],
          "Content-Type": ["text/html"],
        },
        "default-src 'none'",
      ),
    ).toEqual({
      "Content-Security-Policy": ["default-src 'none'"],
      "Content-Type": ["text/html"],
    });
  });

  it("resolves only assets inside the built renderer directory", () => {
    expect(
      resolveRendererAssetPath("/tmp/renderer", "app://renderer/assets/app.js"),
    ).toBe("/tmp/renderer/assets/app.js");
    expect(
      resolveRendererAssetPath("/tmp/renderer", "https://renderer/assets/app.js"),
    ).toBeNull();
    expect(
      resolveRendererAssetPath("/tmp/renderer", "app://renderer/%2e%2e%2fsecret"),
    ).toBeNull();
    expect(
      resolveRendererAssetPath("/tmp/renderer", "app://other/index.html"),
    ).toBeNull();
  });
});
