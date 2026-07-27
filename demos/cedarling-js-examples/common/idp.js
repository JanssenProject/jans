import { createPrivateKey, generateKeyPairSync, randomUUID } from "node:crypto";
import { readFileSync } from "node:fs";
import { fileURLToPath, pathToFileURL } from "node:url";
import path from "node:path";

import express from "express";
import cors from "cors";
import { createLocalJWKSet, jwtVerify, SignJWT } from "jose";
import { errors, Provider } from "oidc-provider";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const DEFAULT_PORT = 9090;
const ONE_HOUR_IN_SECONDS = 60 * 60;
const TWO_WEEKS_IN_SECONDS = 14 * 24 * 60 * 60;
const SIGNING_ALGORITHM = "RS256";
const SIGNING_KEY_ID = "dev-signing-key";
const CORS_ORIGINS_METADATA = "urn:custom:client:allowed-cors-origins";
const KNOWN_CORS_ORIGINS = new Set([
  "http://localhost:3000",
]);
const SUPPORTED_SCOPES = [
  "openid",
  "offline_access",
  "profile",
  "email",
  "address",
  "phone",
  "role",
];
const PROVIDER_ERROR_EVENTS = [
  "authorization.error",
  "backchannel_authentication.error",
  "backchannel.error",
  "challenge.error",
  "code_verification.error",
  "device_authorization.error",
  "device_resume.error",
  "discovery.error",
  "end_session_confirm.error",
  "end_session_success.error",
  "end_session.error",
  "grant.error",
  "introspection.error",
  "jwks.error",
  "pushed_authorization_request.error",
  "registration_create.error",
  "registration_delete.error",
  "registration_read.error",
  "registration_update.error",
  "revocation.error",
  "server_error",
  "userinfo.error",
];
const PROFILE_CLAIMS = [
  "name",
  "family_name",
  "given_name",
  "middle_name",
  "nickname",
  "preferred_username",
  "profile",
  "picture",
  "website",
  "gender",
  "birthdate",
  "zoneinfo",
  "locale",
  "updated_at",
];
const SCOPE_CLAIMS = {
  address: ["address"],
  email: ["email", "email_verified"],
  phone: ["phone_number", "phone_number_verified"],
  profile: PROFILE_CLAIMS,
  role: ["role"],
};

function isWebOrigin(value) {
  if (typeof value !== "string") {
    return false;
  }
  try {
    return new URL(value).origin === value;
  } catch {
    return false;
  }
}

function accountClaims(accountId) {
  const username = accountId.includes("@") ? accountId.split("@")[0] : accountId;
  const isAlice = username === "alice";
  const name = username.charAt(0).toUpperCase() + username.slice(1);
  return {
    sub: username,
    name: name,
    given_name: name,
    nickname: username,
    preferred_username: username,
    email: accountId.includes("@") ? accountId : `${accountId}@example.com`,
    email_verified: true,
    locale: "en",
    role: [isAlice ? "Admin" : "User"],
  };
}

function isClientOriginAllowed(origin, client) {
  return (
    KNOWN_CORS_ORIGINS.has(origin) ||
    client[CORS_ORIGINS_METADATA]?.includes(origin) === true
  );
}

function createSigningKeys() {
  const { privateKey, publicKey } = generateKeyPairSync("rsa", {
    modulusLength: 2048,
  });
  const sharedMetadata = {
    alg: SIGNING_ALGORITHM,
    kid: SIGNING_KEY_ID,
    use: "sig",
  };
  return {
    privateJwk: {
      ...privateKey.export({ format: "jwk" }),
      ...sharedMetadata,
    },
    publicJwk: {
      ...publicKey.export({ format: "jwk" }),
      ...sharedMetadata,
    },
  };
}

function sendInvalidToken(res) {
  res.set(
    "WWW-Authenticate",
    'Bearer error="invalid_token", error_description="invalid token provided"',
  );
  return res.status(401).json({
    error: "invalid_token",
    error_description: "invalid token provided",
  });
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

export function createApp(issuer, logger = console) {
  const { privateJwk, publicJwk } = createSigningKeys();
  const signingKey = createPrivateKey({ key: privateJwk, format: "jwk" });
  const provider = new Provider(issuer, {
    claims: {
      address: ["address"],
      email: ["email", "email_verified"],
      phone: ["phone_number", "phone_number_verified"],
      profile: PROFILE_CLAIMS,
      role: ["role"],
    },
    responseTypes: [
      "code",
      "id_token",
      "id_token token",
      "code id_token",
      "none"
    ],
    clientBasedCORS(_ctx, origin, client) {
      return isClientOriginAllowed(origin, client);
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
          if (!resources || resources.includes(issuer)) {
            return issuer;
          }
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
    extraClientMetadata: {
      properties: [CORS_ORIGINS_METADATA],
      validator(_ctx, key, value, metadata) {
        if (key !== CORS_ORIGINS_METADATA) {
          return;
        }
        if (value === undefined) {
          metadata[CORS_ORIGINS_METADATA] = [];
          return;
        }
        if (!Array.isArray(value) || !value.every(isWebOrigin)) {
          throw new errors.InvalidClientMetadata(
            `${CORS_ORIGINS_METADATA} must be an array of web origins`,
          );
        }
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
  app.use(cors());
  app.use((req, res, next) => {
    res.set("Access-Control-Allow-Origin", "*");
    res.set("Access-Control-Allow-Headers", "Content-Type, Authorization, x-user-id");
    res.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    next();
  });

  const verifyAccessToken = createLocalJWKSet({ keys: [publicJwk] });

  app.use("/me", async (req, res, next) => {
    const authorization = req.get("authorization") ?? "";
    const match = /^Bearer ([^\s]+)$/i.exec(authorization);
    if (
      !["GET", "POST"].includes(req.method) ||
      !match ||
      match[1].split(".").length !== 3
    ) {
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
      if (!client) {
        throw new TypeError("associated client not found");
      }
      if (!grant || grant.isExpired) {
        throw new TypeError("grant is missing or expired");
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
      const origin = req.get("origin");
      res.vary("Origin");
      if (origin) {
        if (!isClientOriginAllowed(origin, client)) {
          throw new TypeError("request origin is not allowed for the client");
        }
        res.set("Access-Control-Allow-Origin", origin);
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
      res.type("application/jwt");
      res.send(userinfoJwt);
    } catch (error) {
      logger.error("[oidc-provider] userinfo.error", error);
      sendInvalidToken(res);
    }
  });

  const policyStorePath = path.resolve(__dirname, "policy-store.json");
  const testConfigPath = path.resolve(__dirname, "test-config.json");
  const policyStore = JSON.parse(readFileSync(policyStorePath, "utf8"));
  const testConfig = JSON.parse(readFileSync(testConfigPath, "utf8"));

  app.get("/config/policy-store", (_req, res) => {
    res.json(policyStore);
  });

  app.get("/config/test-config", (_req, res) => {
    res.json(testConfig);
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
  const app = createApp(issuer);
  const server = app.listen(port, () => {
    console.log(`OIDC server listening at ${issuer}/.well-known/openid-configuration`);
    console.log(`Config endpoints:`);
    console.log(`  http://localhost:${port}/config/policy-store`);
    console.log(`  http://localhost:${port}/config/test-config`);
  });
  server.on("error", (error) => {
    console.error("[http-server] error", error);
  });
  return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  startServer();
}
