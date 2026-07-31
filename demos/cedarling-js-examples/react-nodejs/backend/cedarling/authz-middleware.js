const USERS = new Set(["alice", "bob", "charlie"]);
const ACTIONS = new Set([
  "CreateTask",
  "ViewTask",
  "UpdateTask",
  "DeleteTask",
]);

function identity(req) {
  const userId = req.get("x-user-id");
  if (!userId || !USERS.has(userId)) {
    return { error: "x-user-id must identify alice, bob, or charlie" };
  }
  const authorization = req.get("authorization");
  if (!authorization) return { userId };
  const match = /^Bearer\s+([^\s]+)$/i.exec(authorization);
  return match
    ? { userId, token: match[1] }
    : { error: "Authorization must contain one Bearer token" };
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

export function authorizeMiddleware(cedarling) {
  return function authorize(action) {
    if (!ACTIONS.has(action)) throw new TypeError(`Unsupported action: ${action}`);
    return async (req, res, next) => {
      const requestIdentity = identity(req);
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
          const status = requestIdentity.token ? 401 : 503;
          return res.status(status).json({
            error: requestIdentity.token
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
