import type { CedarEntity } from "@janssenproject/cedarling";
import { ipcMain, shell } from "electron";
import { createServer } from "node:http";

import {
  isUserId,
  MAX_TITLE_LENGTH,
  type CreateTaskRequest,
  type OidcSession,
  type PermissionRequest,
  type TaskAction,
  type TaskRequest,
  type UpdateTaskRequest,
  type UserId,
  type UserRequest,
} from "../shared/contracts";
import { authorizeAction } from "./cedarling/authorize";
import {
  loadCedarlingOptions,
  oidcAllowsInsecureRequests,
  oidcIssuer,
} from "./cedarling/config";
import * as tasks from "./tasks";
import { remoteJwks, verifySignedUserinfoToken } from "./oidc";

const CALLBACK_PORT = 9180;
const REDIRECT_URI = `http://127.0.0.1:${CALLBACK_PORT}/callback`;
const OIDC_TIMEOUT_MS = 120_000;
type OidcModule = typeof import("openid-client");
type OidcConfiguration = Awaited<ReturnType<OidcModule["dynamicClientRegistration"]>>;

let configurationPromise: Promise<OidcConfiguration> | undefined;
let loginInProgress = false;
let signedSession: { userId: UserId; token: string } | undefined;

function record(value: unknown, keys: readonly string[]): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new TypeError("IPC payload must be an object");
  }
  const result = value as Record<string, unknown>;
  if (Object.keys(result).some((key) => !keys.includes(key))) {
    throw new TypeError("IPC payload contains an unsupported field");
  }
  return result;
}

function userId(value: unknown): UserId {
  if (!isUserId(value)) {
    throw new TypeError("Unknown example user");
  }
  return value;
}

function nonEmpty(value: unknown, name: string): string {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new TypeError(`${name} must be a non-empty string`);
  }
  return value.trim();
}

export function parseUserRequest(value: unknown): UserRequest {
  const input = record(value, ["userId"]);
  return { userId: userId(input.userId) };
}

export function parseCreateRequest(value: unknown): CreateTaskRequest {
  const input = record(value, ["userId", "title"]);
  const title = nonEmpty(input.title, "title");
  if (title.length > MAX_TITLE_LENGTH) throw new TypeError("title must not exceed 120 characters");
  return { userId: userId(input.userId), title };
}

export function parseTaskRequest(value: unknown): TaskRequest {
  const input = record(value, ["userId", "id"]);
  return { userId: userId(input.userId), id: nonEmpty(input.id, "id") };
}

export function parseUpdateRequest(value: unknown): UpdateTaskRequest {
  const input = record(value, ["userId", "id", "title", "completed"]);
  if (input.title === undefined && input.completed === undefined) {
    throw new TypeError("update requires title and/or completed");
  }
  if (input.completed !== undefined && typeof input.completed !== "boolean") {
    throw new TypeError("completed must be boolean");
  }
  const title = input.title === undefined ? undefined : nonEmpty(input.title, "title");
  if (title && title.length > MAX_TITLE_LENGTH) throw new TypeError("title must not exceed 120 characters");
  return {
    userId: userId(input.userId),
    id: nonEmpty(input.id, "id"),
    ...(title === undefined ? {} : { title }),
    ...(typeof input.completed === "boolean" ? { completed: input.completed } : {}),
  };
}

function parsePermissionRequest(value: unknown): PermissionRequest {
  const input = record(value, ["userId", "id", "action"]);
  const action = input.action;
  if (action !== "UpdateTask" && action !== "DeleteTask") {
    throw new TypeError("Unsupported permission action");
  }
  return { ...parseTaskRequest({ userId: input.userId, id: input.id }), action };
}

function taskResource(task: tasks.Task): CedarEntity {
  // Main reconstructs the Cedar resource from its own task store; renderer
  // input can select a task but cannot forge its authorization attributes.
  return {
    type: "TaskApp::Task",
    id: task.id,
    attributes: { owner: task.owner, title: task.title, completed: task.completed },
  };
}

async function authorizeForUser(action: TaskAction, requestedUser: UserId, resource: CedarEntity) {
  if (signedSession && signedSession.userId !== requestedUser) return "denied";
  return authorizeAction(action, requestedUser, resource, signedSession?.token);
}

async function requireAuthorization(action: TaskAction, requestedUser: UserId, resource: CedarEntity) {
  const outcome = await authorizeForUser(action, requestedUser, resource);
  if (outcome === "denied") throw new Error("Forbidden by policy");
  if (outcome === "error") throw new Error("Authorization service unavailable");
}

async function getOidcClient(): Promise<OidcConfiguration> {
  configurationPromise ??= (async () => {
    const oidc = await import("openid-client");
    return oidc.dynamicClientRegistration(
      new URL(oidcIssuer()),
      {
        application_type: "native",
        client_name: "Cedarling JS for Electron",
        grant_types: ["authorization_code"],
        redirect_uris: [REDIRECT_URI],
        response_types: ["code"],
        token_endpoint_auth_method: "none",
        userinfo_signed_response_alg: "RS256",
      },
      oidc.None(),
      oidcAllowsInsecureRequests() ? { execute: [oidc.allowInsecureRequests] } : undefined,
    );
  })().catch((error) => {
    configurationPromise = undefined;
    throw error;
  });
  return configurationPromise;
}

function safeAuthorizationUrl(value: URL): URL {
  const issuer = new URL(oidcIssuer());
  if (value.origin !== issuer.origin || !["http:", "https:"].includes(value.protocol)) {
    throw new Error("OIDC authorization endpoint is not trusted");
  }
  return value;
}

function callbackPage(title: string, copy: string): string {
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title}</title><style>
    :root{color-scheme:light}*{box-sizing:border-box}body{margin:0;min-height:100vh;display:grid;place-items:center;padding:1rem;background:#fafbfe;color:#34495c;font-family:Lato,"Open Sans",Arial,sans-serif}
    main{width:min(100%,28rem);overflow:hidden;border:1px solid #dfe5e8;border-radius:.65rem;background:#fff;box-shadow:0 .2rem .75rem rgb(52 73 92/5%)}
    i{display:block;height:.3rem;background:#15b565}section{padding:2rem}p:first-child{margin:0 0 .25rem;color:#087846;font-size:.7rem;font-weight:700;letter-spacing:.12em;text-transform:uppercase}
    h1{margin:0;font-size:1.75rem}p:last-child{margin:.75rem 0 0;color:#526675;line-height:1.55}
  </style></head><body><main><i aria-hidden="true"></i><section><p>TaskApp desktop</p><h1>${title}</h1><p>${copy}</p></section></main></body></html>`;
}

async function login(requestedUser: UserId): Promise<OidcSession> {
  if (loginInProgress) throw new Error("Another OIDC login is already running");
  loginInProgress = true;
  try {
    const oidc = await import("openid-client");
    const config = await getOidcClient();
    const codeVerifier = oidc.randomPKCECodeVerifier();
    const codeChallenge = await oidc.calculatePKCECodeChallenge(codeVerifier);
    const state = oidc.randomState();
    const nonce = oidc.randomNonce();
    const authorizationUrl = safeAuthorizationUrl(
      oidc.buildAuthorizationUrl(config, {
        code_challenge: codeChallenge,
        code_challenge_method: "S256",
        login_hint: requestedUser,
        nonce,
        redirect_uri: REDIRECT_URI,
        resource: oidcIssuer(),
        response_type: "code",
        scope: "openid profile role",
        state,
      }),
    );

    const token = await new Promise<string>((resolve, reject) => {
      let settled = false;
      const finish = (callback: () => void) => {
        if (settled) return;
        settled = true;
        callback();
        server.close();
      };
      const server = createServer(async (request, response) => {
        const callbackUrl = new URL(request.url ?? "/", REDIRECT_URI);
        if (callbackUrl.pathname !== "/callback") {
          response.writeHead(404).end("Not found");
          return;
        }
        try {
          const tokenSet = await oidc.authorizationCodeGrant(config, callbackUrl, {
            expectedNonce: nonce,
            expectedState: state,
            pkceCodeVerifier: codeVerifier,
          });
          const subject = tokenSet.claims()?.sub;
          if (subject !== requestedUser || typeof tokenSet.access_token !== "string") {
            throw new Error("OIDC subject mismatch or missing access token");
          }
          const userinfo = await fetch(`${oidcIssuer()}/me`, {
            headers: { accept: "application/jwt", authorization: `Bearer ${tokenSet.access_token}` },
          });
          const userinfoToken = await userinfo.text();
          if (!userinfo.ok || userinfoToken.split(".").length !== 3) {
            throw new Error("OIDC provider did not return signed UserInfo");
          }
          const metadata = config.serverMetadata();
          const clientId = config.clientMetadata().client_id;
          if (typeof metadata.jwks_uri !== "string" || typeof clientId !== "string") {
            throw new Error("OIDC provider metadata is incomplete");
          }
          await verifySignedUserinfoToken(userinfoToken, await remoteJwks(metadata.jwks_uri), {
            audience: clientId,
            issuer: oidcIssuer(),
            subject: requestedUser,
          });
          // Only after independent JWT verification does main retain the
          // UserInfo token for Cedarling's signed authorization path.
          response.writeHead(200, {
            "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'",
            "Content-Type": "text/html; charset=utf-8",
            "X-Content-Type-Options": "nosniff",
          });
          response.end(callbackPage("Signed in", "Sign-in is complete. You can return to TaskApp."));
          finish(() => resolve(userinfoToken));
        } catch (error) {
          response.writeHead(400, {
            "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'",
            "Content-Type": "text/html; charset=utf-8",
            "X-Content-Type-Options": "nosniff",
          });
          response.end(callbackPage("Sign-in failed", "The OIDC callback could not be validated. Return to TaskApp and try again."));
          finish(() => reject(error));
        }
      });
      const timeout = setTimeout(
        () => finish(() => reject(new Error("OIDC login timed out"))),
        OIDC_TIMEOUT_MS,
      );
      server.on("close", () => clearTimeout(timeout));
      server.on("error", (error) => finish(() => reject(error)));
      server.listen(CALLBACK_PORT, "127.0.0.1", () => {
        void shell.openExternal(authorizationUrl.href).catch((error) => finish(() => reject(error)));
      });
    });
    signedSession = { userId: requestedUser, token };
    return { authenticated: true, userId: requestedUser };
  } finally {
    loginInProgress = false;
  }
}

export function registerIpcHandlers(registrar = ipcMain): void {
  const handle = (channel: string, handler: (...args: unknown[]) => unknown) => {
    registrar.removeHandler(channel);
    registrar.handle(channel, handler);
  };
  // Renderer Cedarling receives policy configuration through this narrow
  // bridge, but signed tokens stay private to main.
  handle("cedarling:options", () => loadCedarlingOptions());
  handle("cedarling:signed-permission", async (_event, value) => {
    const request = parsePermissionRequest(value);
    const task = tasks.findById(request.id);
    if (!task) throw new Error("Task not found");
    if (!signedSession) return false;
    // Signed permission previews cross IPC because only main can access the
    // signed UserInfo token.
    return (await authorizeForUser(request.action, request.userId, taskResource(task))) === "allowed";
  });
  handle("tasks:list", async (_event, value) => {
    const request = parseUserRequest(value);
    await Promise.all(tasks.getAll().map((task) => requireAuthorization("ViewTask", request.userId, taskResource(task))));
    return tasks.getAll();
  });
  handle("tasks:create", async (_event, value) => {
    const request = parseCreateRequest(value);
    // Authorization precedes mutation, and the resource owner comes from the
    // validated request identity.
    await requireAuthorization("CreateTask", request.userId, {
      type: "TaskApp::Task",
      id: "new-task",
      attributes: { owner: request.userId, title: request.title, completed: false },
    });
    return tasks.create(request.title, request.userId);
  });
  handle("tasks:update", async (_event, value) => {
    const request = parseUpdateRequest(value);
    const task = tasks.findById(request.id);
    if (!task) throw new Error("Task not found");
    await requireAuthorization("UpdateTask", request.userId, taskResource(task));
    return tasks.update(task.id, { title: request.title, completed: request.completed });
  });
  handle("tasks:delete", async (_event, value) => {
    const request = parseTaskRequest(value);
    const task = tasks.findById(request.id);
    if (!task) throw new Error("Task not found");
    await requireAuthorization("DeleteTask", request.userId, taskResource(task));
    tasks.remove(task.id);
  });
  handle("oidc:session", () => ({
    authenticated: Boolean(signedSession),
    ...(signedSession ? { userId: signedSession.userId } : {}),
  }));
  handle("oidc:login", (_event, value) => login(parseUserRequest(value).userId));
  handle("oidc:logout", () => {
    signedSession = undefined;
    return { authenticated: false };
  });
}

registerIpcHandlers();
