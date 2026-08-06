import { createPrivateKey, generateKeyPairSync, randomBytes, randomUUID } from "node:crypto";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

import express from "express";
import { createLocalJWKSet, jwtVerify, SignJWT } from "jose";
import { errors, Provider } from "oidc-provider";

import { createPolicyStore } from "./policy-store.js";

const directory = path.dirname(fileURLToPath(import.meta.url));
const DEFAULT_PORT = 9090;
const DEFAULT_FRONTEND_ORIGIN = "http://localhost:3000";
const ONE_HOUR_IN_SECONDS = 60 * 60;
const TWO_WEEKS_IN_SECONDS = 14 * 24 * 60 * 60;
const SIGNING_ALGORITHM = "RS256";
const SIGNING_KEY_ID = "dev-signing-key";
const SUPPORTED_SCOPES = ["openid", "profile", "role"];
const PROFILE_CLAIMS = ["name", "preferred_username"];
const SCOPE_CLAIMS = {
  profile: PROFILE_CLAIMS,
  role: ["role"],
};
const PROVIDER_ERROR_EVENTS = [
  "authorization.error",
  "code_verification.error",
  "discovery.error",
  "grant.error",
  "jwks.error",
  "registration_create.error",
  "server_error",
  "userinfo.error",
];

function isLoopback(hostname) {
  return (
    hostname === "localhost" ||
    hostname === "127.0.0.1" ||
    hostname === "[::1]"
  );
}

function validatedOrigin(value, name) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new TypeError(`${name} must be an absolute HTTP(S) origin`);
  }
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.search ||
    url.hash ||
    url.pathname !== "/" ||
    (url.protocol !== "https:" && !isLoopback(url.hostname))
  ) {
    throw new TypeError(
      `${name} must be an HTTPS origin (loopback HTTP is allowed)`,
    );
  }
  return url.origin;
}

function accountClaims(accountId) {
  const username = accountId.includes("@") ? accountId.split("@")[0] : accountId;
  const name = username.charAt(0).toUpperCase() + username.slice(1);
  return {
    sub: username,
    name,
    preferred_username: username,
    role: [username === "alice" ? "Admin" : "User"],
  };
}

function createSigningKeys() {
  const { privateKey, publicKey } = generateKeyPairSync("rsa", {
    modulusLength: 2048,
  });
  const metadata = {
    alg: SIGNING_ALGORITHM,
    kid: SIGNING_KEY_ID,
    use: "sig",
  };
  return {
    privateJwk: { ...privateKey.export({ format: "jwk" }), ...metadata },
    publicJwk: { ...publicKey.export({ format: "jwk" }), ...metadata },
  };
}

function selectUserinfoClaims(subject, accountId, scopes) {
  const availableClaims = accountClaims(accountId);
  const selectedClaims = { sub: subject };
  for (const scope of scopes) {
    for (const claim of SCOPE_CLAIMS[scope] ?? []) {
      if (availableClaims[claim] !== undefined) {
        selectedClaims[claim] = availableClaims[claim];
      }
    }
  }
  return selectedClaims;
}

function sendInvalidToken(res) {
  res.set(
    "WWW-Authenticate",
    "Bearer error=\"invalid_token\", error_description=\"invalid token provided\"",
  );
  return res.status(401).json({
    error: "invalid_token",
    error_description: "invalid token provided",
  });
}

function exactOriginCors(allowedOrigins) {
  return (req, res, next) => {
    const origin = req.get("origin");
    res.vary("Origin");
    if (origin && allowedOrigins.has(origin)) {
      res.set("Access-Control-Allow-Origin", origin);
      res.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
      res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }
    if (req.method === "OPTIONS") {
      return origin && allowedOrigins.has(origin)
        ? res.sendStatus(204)
        : res.status(403).json({ error: "origin_not_allowed" });
    }
    next();
  };
}

export function createApp(
  issuerValue,
  { frontendOrigin = DEFAULT_FRONTEND_ORIGIN, logger = console } = {},
) {
  const issuer = validatedOrigin(issuerValue, "OIDC_ISSUER");
  const allowedOrigins = new Set([
    validatedOrigin(frontendOrigin, "FRONTEND_ORIGIN"),
  ]);
  const { privateJwk, publicJwk } = createSigningKeys();
  const signingKey = createPrivateKey({ key: privateJwk, format: "jwk" });
  const provider = new Provider(issuer, {
    cookies: { keys: [randomBytes(32).toString("base64url")] },
    claims: {
      profile: PROFILE_CLAIMS,
      role: ["role"],
    },
    responseTypes: ["code"],
    clientBasedCORS(_ctx, origin) {
      return allowedOrigins.has(origin);
    },
    clients: [],
    clientDefaults: {
      userinfo_signed_response_alg: SIGNING_ALGORITHM,
    },
    features: {
      registration: { enabled: true },
      jwtUserinfo: { enabled: true },
      resourceIndicators: {
        enabled: true,
        async defaultResource(_ctx, _client, resources) {
          if (!resources || resources.includes(issuer)) return issuer;
          throw new errors.InvalidTarget("unsupported resource");
        },
        async getResourceServerInfo(_ctx, resourceIndicator) {
          if (resourceIndicator !== issuer) {
            throw new errors.InvalidTarget("unsupported resource");
          }
          return {
            accessTokenFormat: "jwt",
            audience: issuer,
            jwt: { sign: { alg: SIGNING_ALGORITHM, kid: SIGNING_KEY_ID } },
            scope: SUPPORTED_SCOPES.join(" "),
          };
        },
        async useGrantedResource() {
          return true;
        },
      },
    },
    formats: {
      customizers: {
        async jwt(_ctx, token, jwt) {
          jwt.payload.grant_id = token.grantId;
        },
      },
    },
    async findAccount(_ctx, accountId) {
      return {
        accountId,
        async claims() {
          return accountClaims(accountId);
        },
      };
    },
    jwks: { keys: [privateJwk] },
    scopes: SUPPORTED_SCOPES,
    ttl: {
      AccessToken: ONE_HOUR_IN_SECONDS,
      AuthorizationCode: 60,
      Grant: TWO_WEEKS_IN_SECONDS,
      IdToken: ONE_HOUR_IN_SECONDS,
      Interaction: ONE_HOUR_IN_SECONDS,
      RefreshToken: TWO_WEEKS_IN_SECONDS,
      Session: TWO_WEEKS_IN_SECONDS,
    },
  });

  for (const eventName of PROVIDER_ERROR_EVENTS) {
    provider.on(eventName, (_ctx, error) => {
      logger.error(`[oidc-provider] ${eventName}`, error);
    });
  }

  const app = express();
  app.disable("x-powered-by");
  app.use(exactOriginCors(allowedOrigins));
  const verifyAccessToken = createLocalJWKSet({ keys: [publicJwk] });

  // Cedarling consumes signed UserInfo JWTs, so this endpoint validates the
  // access token and returns only the claims granted to this OIDC client.
  app.use("/me", async (req, res, next) => {
    const match = /^Bearer ([^\s]+)$/i.exec(req.get("authorization") ?? "");
    if (!["GET", "POST"].includes(req.method) || !match) {
      next();
      return;
    }
    try {
      const { payload } = await jwtVerify(match[1], verifyAccessToken, {
        algorithms: [SIGNING_ALGORITHM],
        audience: issuer,
        issuer,
        requiredClaims: [
          "client_id",
          "exp",
          "grant_id",
          "iat",
          "jti",
          "scope",
          "sub",
        ],
        typ: "at+jwt",
      });
      if (
        typeof payload.client_id !== "string" ||
        typeof payload.grant_id !== "string" ||
        typeof payload.scope !== "string" ||
        typeof payload.sub !== "string" ||
        payload.cnf !== undefined
      ) {
        throw new TypeError("invalid access token claims");
      }
      const tokenScopes = new Set(payload.scope.split(" ").filter(Boolean));
      if (!tokenScopes.has("openid")) {
        throw new TypeError("access token missing openid scope");
      }
      const [client, grant] = await Promise.all([
        provider.Client.find(payload.client_id),
        provider.Grant.find(payload.grant_id, { ignoreExpiration: true }),
      ]);
      if (!client || !grant || grant.isExpired) {
        throw new TypeError("associated client or grant is unavailable");
      }
      if (grant.clientId !== payload.client_id || grant.accountId !== payload.sub) {
        throw new TypeError("access token grant mismatch");
      }
      const grantedScopes = new Set(
        grant.getOIDCScopeFiltered(tokenScopes).split(" ").filter(Boolean),
      );
      if (!grantedScopes.has("openid")) {
        throw new TypeError("grant is missing openid scope");
      }
      res.set("Cache-Control", "no-store");
      res.set("Pragma", "no-cache");
      const { sub, ...claims } = selectUserinfoClaims(
        payload.sub,
        grant.accountId,
        grantedScopes,
      );
      const userinfoJwt = await new SignJWT(claims)
        .setProtectedHeader({
          alg: SIGNING_ALGORITHM,
          kid: SIGNING_KEY_ID,
          typ: "JWT",
        })
        .setIssuer(issuer)
        .setAudience(client.clientId)
        .setSubject(sub)
        .setJti(randomUUID())
        .setIssuedAt()
        .setExpirationTime(payload.exp)
        .sign(signingKey);
      res.type("application/jwt").send(userinfoJwt);
    } catch (error) {
      logger.error("[oidc-provider] userinfo.error", error);
      sendInvalidToken(res);
    }
  });

  const cedarlingConfig = JSON.parse(
    readFileSync(path.join(directory, "cedarling-config.json"), "utf8"),
  );
  // Both documents are generated for the effective issuer so every runtime
  // validates the same signed tokens without hard-coding a machine address.
  app.get("/config/policy-store", (_req, res) => {
    res.json(createPolicyStore(issuer));
  });
  app.get("/config/cedarling", (_req, res) => {
    res.json(cedarlingConfig);
  });
  app.use("/", provider.callback());
  return app;
}

function readPort(value) {
  const port = Number(value);
  if (!Number.isInteger(port) || port < 1 || port > 65_535) {
    throw new TypeError("PORT must be an integer between 1 and 65535");
  }
  return port;
}

export function startServer() {
  const port = readPort(process.env.PORT ?? DEFAULT_PORT);
  const issuer = process.env.OIDC_ISSUER ?? `http://localhost:${port}`;
  const app = createApp(issuer, {
    frontendOrigin: process.env.FRONTEND_ORIGIN ?? DEFAULT_FRONTEND_ORIGIN,
  });
  const server = app.listen(port, () => {
    console.log(`OIDC discovery: ${issuer}/.well-known/openid-configuration`);
    console.log(`Cedarling config: ${issuer}/config/cedarling`);
    console.log(`Policy store: ${issuer}/config/policy-store`);
  });
  server.on("error", (error) => console.error("[http-server] error", error));
  return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  startServer();
}
