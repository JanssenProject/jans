import { createRemoteJWKSet, jwtVerify } from "jose";

const TRANSACTION_KEY = "taskapp_oidc_transaction";
const SESSION_KEY = "taskapp_oidc_session";

interface Discovery {
  authorization_endpoint: string;
  issuer: string;
  jwks_uri: string;
  registration_endpoint: string;
  token_endpoint: string;
  userinfo_endpoint: string;
}

interface Transaction {
  clientId: string;
  codeVerifier: string;
  nonce: string;
  redirectUri: string;
  requestedUser: string;
  state: string;
  startedAt: number;
}

export interface SignedSession {
  clientId: string;
  userId: string;
  userinfoToken: string;
}

function loopback(hostname: string): boolean {
  return ["localhost", "127.0.0.1", "[::1]"].includes(hostname);
}

export function validatedOrigin(value: string, name = "URL"): string {
  const url = new URL(value);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.origin !== value ||
    (url.protocol !== "https:" && !loopback(url.hostname))
  ) {
    throw new Error(`${name} must be an HTTPS origin (loopback HTTP is allowed)`);
  }
  return url.origin;
}

export function oidcIssuer(): string {
  return validatedOrigin(
    import.meta.env.VITE_OIDC_ISSUER ?? "http://localhost:9090",
    "VITE_OIDC_ISSUER",
  );
}

function endpoint(value: unknown, name: string): string {
  if (typeof value !== "string") throw new Error(`OIDC discovery is missing ${name}`);
  const url = new URL(value);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    (url.protocol !== "https:" && !loopback(url.hostname))
  ) {
    throw new Error(`OIDC ${name} is not a secure HTTP(S) URL`);
  }
  return url.href;
}

async function discovery(): Promise<Discovery> {
  const issuer = oidcIssuer();
  const response = await fetch(`${issuer}/.well-known/openid-configuration`);
  if (!response.ok) throw new Error(`OIDC discovery failed: HTTP ${response.status}`);
  const value = (await response.json()) as Record<string, unknown>;
  if (value.issuer !== issuer) throw new Error("OIDC discovery issuer mismatch");
  return {
    issuer,
    authorization_endpoint: endpoint(value.authorization_endpoint, "authorization_endpoint"),
    jwks_uri: endpoint(value.jwks_uri, "jwks_uri"),
    registration_endpoint: endpoint(value.registration_endpoint, "registration_endpoint"),
    token_endpoint: endpoint(value.token_endpoint, "token_endpoint"),
    userinfo_endpoint: endpoint(value.userinfo_endpoint, "userinfo_endpoint"),
  };
}

function randomBase64Url(length = 32): string {
  const bytes = crypto.getRandomValues(new Uint8Array(length));
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

export async function createPkce(): Promise<{ verifier: string; challenge: string }> {
  const verifier = randomBase64Url(64);
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
  const challenge = btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
  return { verifier, challenge };
}

function readStored<T>(key: string): T | null {
  const value = sessionStorage.getItem(key);
  if (!value) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    sessionStorage.removeItem(key);
    return null;
  }
}

export function getSignedSession(): SignedSession | null {
  const session = readStored<SignedSession>(SESSION_KEY);
  return session && typeof session.userId === "string" && typeof session.userinfoToken === "string"
    ? session
    : null;
}

export function clearSignedSession(): void {
  sessionStorage.removeItem(SESSION_KEY);
  sessionStorage.removeItem(TRANSACTION_KEY);
}

export async function startLogin(requestedUser: string): Promise<void> {
  const metadata = await discovery();
  const redirectUri = new URL("/callback", window.location.origin).href;
  const registrationResponse = await fetch(metadata.registration_endpoint, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      application_type: "web",
      client_name: "TaskApp React example",
      grant_types: ["authorization_code"],
      redirect_uris: [redirectUri],
      response_types: ["code"],
      token_endpoint_auth_method: "none",
      userinfo_signed_response_alg: "RS256",
    }),
  });
  const registration = (await registrationResponse.json()) as Record<string, unknown>;
  if (!registrationResponse.ok || typeof registration.client_id !== "string") {
    throw new Error("OIDC dynamic client registration failed");
  }
  const { verifier, challenge } = await createPkce();
  const transaction: Transaction = {
    clientId: registration.client_id,
    codeVerifier: verifier,
    nonce: randomBase64Url(),
    redirectUri,
    requestedUser,
    state: randomBase64Url(),
    startedAt: Date.now(),
  };
  sessionStorage.setItem(TRANSACTION_KEY, JSON.stringify(transaction));
  const authorizationUrl = new URL(metadata.authorization_endpoint);
  authorizationUrl.search = new URLSearchParams({
    client_id: transaction.clientId,
    code_challenge: challenge,
    code_challenge_method: "S256",
    login_hint: requestedUser,
    nonce: transaction.nonce,
    redirect_uri: redirectUri,
    resource: metadata.issuer,
    response_type: "code",
    scope: "openid profile role",
    state: transaction.state,
  }).toString();
  window.location.assign(authorizationUrl);
}

export async function completeLogin(): Promise<SignedSession | null> {
  const query = new URLSearchParams(window.location.search);
  const code = query.get("code");
  const returnedState = query.get("state");
  const providerError = query.get("error");
  if (!code && !returnedState && !providerError) return null;
  const transaction = readStored<Transaction>(TRANSACTION_KEY);
  sessionStorage.removeItem(TRANSACTION_KEY);
  window.history.replaceState(null, "", "/");
  if (providerError) throw new Error(`OIDC authorization failed: ${providerError}`);
  if (
    !transaction ||
    !code ||
    returnedState !== transaction.state ||
    Date.now() - transaction.startedAt > 10 * 60 * 1000
  ) {
    throw new Error("OIDC transaction is missing, expired, or has invalid state");
  }
  const metadata = await discovery();
  const tokenResponse = await fetch(metadata.token_endpoint, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: transaction.clientId,
      code,
      code_verifier: transaction.codeVerifier,
      grant_type: "authorization_code",
      redirect_uri: transaction.redirectUri,
    }),
  });
  const tokens = (await tokenResponse.json()) as Record<string, unknown>;
  if (
    !tokenResponse.ok ||
    typeof tokens.access_token !== "string" ||
    typeof tokens.id_token !== "string"
  ) {
    throw new Error("OIDC token exchange failed");
  }
  const jwks = createRemoteJWKSet(new URL(metadata.jwks_uri));
  const idToken = await jwtVerify(tokens.id_token, jwks, {
    algorithms: ["RS256"],
    audience: transaction.clientId,
    issuer: metadata.issuer,
  });
  if (idToken.payload.nonce !== transaction.nonce || typeof idToken.payload.sub !== "string") {
    throw new Error("OIDC ID token nonce or subject is invalid");
  }
  const userinfoResponse = await fetch(metadata.userinfo_endpoint, {
    headers: { authorization: `Bearer ${tokens.access_token}` },
  });
  const userinfoToken = await userinfoResponse.text();
  if (!userinfoResponse.ok || userinfoToken.split(".").length !== 3) {
    throw new Error("OIDC provider did not return signed UserInfo");
  }
  const userinfo = await jwtVerify(userinfoToken, jwks, {
    algorithms: ["RS256"],
    audience: transaction.clientId,
    issuer: metadata.issuer,
    subject: idToken.payload.sub,
  });
  if (userinfo.payload.sub !== transaction.requestedUser) {
    throw new Error("Authenticated subject does not match the requested example user");
  }
  // Retain only the verified signed UserInfo JWT needed by Cedarling. The SDK
  // verifies it again when authorizeMultiIssuer evaluates token-based policy.
  const session = {
    clientId: transaction.clientId,
    userId: transaction.requestedUser,
    userinfoToken,
  };
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}
