import { createRemoteJWKSet, jwtVerify } from "jose";

const USERS = new Set(["alice", "bob", "charlie"]);
const ACTIONS = new Set([
  "CreateTask",
  "ViewTask",
  "UpdateTask",
  "DeleteTask",
]);

let jwksPromise;

function getJwks(issuerOrigin) {
  jwksPromise ??= (async () => {
    const response = await fetch(`${issuerOrigin}/.well-known/openid-configuration`, {
      signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) throw new Error(`OIDC discovery failed: HTTP ${response.status}`);
    const { jwks_uri } = await response.json();
    return createRemoteJWKSet(new URL(jwks_uri));
  })().catch((error) => {
    jwksPromise = undefined;
    throw error;
  });
  return jwksPromise;
}

export function createVerifyTokenSub(issuerOrigin) {
  return async function verifyTokenSub(token) {
    const jwks = await getJwks(issuerOrigin);
    const { payload } = await jwtVerify(token, jwks, { algorithms: ["RS256"] });
    return payload.sub;
  };
}

async function identity(req, verifyTokenSub, allowUnsignedDemoIdentity) {
  const authorization = req.get("authorization");

  if (authorization !== undefined) {
    const match = /^Bearer\s+([^\s]+)$/i.exec(authorization);
    if (!match) {
      return { error: "Authorization must contain one Bearer token" };
    }
    let sub;
    try {
      sub = await verifyTokenSub(match[1]);
    } catch {
      return { error: "Invalid or expired signed token" };
    }
    if (!sub || !USERS.has(sub)) {
      return { error: "Token subject must identify alice, bob, or charlie" };
    }
    return { userId: sub, token: match[1] };
  }

  if (!allowUnsignedDemoIdentity) {
    return { error: "Unsigned development identity is disabled" };
  }
  const headerUserId = req.get("x-user-id");
  if (!headerUserId || !USERS.has(headerUserId)) {
    return { error: "x-user-id must identify alice, bob, or charlie" };
  }
  return { userId: headerUserId };
}

function resourceFor(req, action) {
  // Resource attributes come from server-owned task state, never from an
  // untrusted client-supplied Cedar entity.
  const task = req.task;
  if (task) {
    return {
      type: "TaskApp::Task",
      id: task.id,
      attributes: {
        owner: task.owner,
        title: task.title,
        completed: task.completed,
      },
    };
  }
  return {
    type: "TaskApp::Task",
    id: action === "CreateTask" ? "new-task" : "task-collection",
    attributes: {
      owner: req.identity.userId,
      title: action === "CreateTask" ? req.body.title : "Tasks",
      completed: false,
    },
  };
}

export function authorizeMiddleware(
  cedarling,
  { allowUnsignedDemoIdentity = false, verifyTokenSub } = {},
) {
  return function authorize(action) {
    if (!ACTIONS.has(action)) throw new TypeError(`Unsupported action: ${action}`);
    return async (req, res, next) => {
      const requestIdentity = await identity(
        req,
        verifyTokenSub,
        allowUnsignedDemoIdentity,
      );
      if (requestIdentity.error) {
        return res.status(401).json({ error: requestIdentity.error });
      }
      req.identity = requestIdentity;
      const resource = resourceFor(req, action);
      try {
        // Signed mode asks Cedarling to verify and map UserInfo. Unsigned mode
        // supplies the application's explicit principal and context directly.
        const result = requestIdentity.token
          ? await cedarling.authorizeMultiIssuer({
              tokens: [
                {
                  mapping: "LocalMockIdP::Userinfo_token",
                  payload: requestIdentity.token,
                },
              ],
              action: `TaskApp::Action::"${action}"`,
              resource,
              context: {},
            })
          : await cedarling.authorizeUnsigned({
              principal: { type: "TaskApp::User", id: requestIdentity.userId },
              action: `TaskApp::Action::"${action}"`,
              resource,
              context: { userId: requestIdentity.userId },
            });
        if (!result.ok) {
          const signedFailure =
            Boolean(requestIdentity.token) && result.error.code === "AUTHORIZATION_FAILED";
          return res.status(signedFailure ? 401 : 503).json({
            error: signedFailure
              ? "Invalid or expired signed identity"
              : "Authorization service unavailable",
          });
        }
        // A successful Result means evaluation completed; the decision remains
        // the policy enforcement point.
        if (!result.value.decision) {
          return res.status(403).json({ error: "Forbidden by policy" });
        }
        req.userId = requestIdentity.userId;
        next();
      } catch {
        res.status(503).json({ error: "Authorization service unavailable" });
      }
    };
  };
}
