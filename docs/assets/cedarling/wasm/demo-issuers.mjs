// The demo signs tokens locally and serves each issuer's public key over HTTP.
import { generateKeyPairSync, randomUUID, sign } from "node:crypto";
import { once } from "node:events";
import { createServer } from "node:http";

function encode(value) {
  return Buffer.from(JSON.stringify(value)).toString("base64url");
}

function createIssuer(name, origin) {
  const issuer = origin + "/" + name;
  const { privateKey, publicKey } = generateKeyPairSync("ec", { namedCurve: "P-256" });
  const kid = randomUUID();
  const jwk = { ...publicKey.export({ format: "jwk" }), kid, use: "sig", alg: "ES256" };
  const now = Math.floor(Date.now() / 1000);
  const header = encode({ alg: "ES256", typ: "JWT", kid });
  const claims = encode({
    iss: issuer, sub: "alice", jti: randomUUID(), iat: now, exp: now + 300,
    token_type: "Bearer", org_id: "example",
  });
  const input = header + "." + claims;
  const signature = sign("sha256", Buffer.from(input), {
    key: privateKey, dsaEncoding: "ieee-p1363",
  });
  return {
    discovery: { issuer, jwks_uri: issuer + "/jwks" },
    jwks: { keys: [jwk] },
    token: { mapping: name + "::Access_Token", payload: input + "." + signature.toString("base64url") },
  };
}

export async function startDemoIssuers() {
  const routes = new Map();
  const server = createServer((req, res) => {
    const body = routes.get(req.url);
    if (req.method !== "GET" || body === undefined) {
      res.writeHead(404).end();
      return;
    }
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify(body));
  });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  const origin = "http://127.0.0.1:" + server.address().port;
  const close = () => new Promise((resolve, reject) =>
    server.close(error => error ? reject(error) : resolve()));
  try {
    const tokens = ["Acme", "Dolphin"].map(name => {
      const issuer = createIssuer(name, origin);
      routes.set("/" + name + "/.well-known/openid-configuration", issuer.discovery);
      routes.set("/" + name + "/jwks", issuer.jwks);
      return issuer.token;
    });
    return { origin, tokens, close };
  } catch (error) {
    await close();
    throw error;
  }
}
