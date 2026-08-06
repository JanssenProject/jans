import { Hono, type Context, type Next } from "hono";

import {
  authorizeAction,
  collectionResource,
  newTaskResource,
  taskResource,
  type AuthorizationInput,
  type AuthorizationOutcome,
  type TaskAction,
} from "./cedarling/authorize";
import { createTaskStore } from "./tasks";

export interface Bindings {
  FRONTEND_ORIGIN?: string;
  OIDC_ISSUER?: string;
}

interface Identity {
  token?: string;
  userId: string;
}

type Authorize = (input: AuthorizationInput) => Promise<AuthorizationOutcome>;
const USERS = new Set(["alice", "bob", "charlie"]);
const MAX_TITLE_LENGTH = 120;

function environment(c: Context<{ Bindings: Bindings }>) {
  return {
    frontendOrigin: new URL(c.env.FRONTEND_ORIGIN ?? "http://localhost:3000").origin,
    issuer: c.env.OIDC_ISSUER ?? "http://localhost:9090",
  };
}

function identity(c: Context): Identity | undefined {
  const userId = c.req.header("x-user-id");
  if (!userId || !USERS.has(userId)) return undefined;
  const authorization = c.req.header("authorization");
  if (!authorization) return { userId };
  const match = /^Bearer\s+([^\s]+)$/i.exec(authorization);
  return match ? { userId, token: match[1] } : undefined;
}

function validTitle(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0 && value.trim().length <= MAX_TITLE_LENGTH;
}

async function jsonBody(c: Context): Promise<Record<string, unknown> | undefined> {
  try {
    const value: unknown = await c.req.json();
    return value && typeof value === "object" && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : undefined;
  } catch {
    return undefined;
  }
}

function authorizationResponse(c: Context, outcome: AuthorizationOutcome) {
  if (outcome.kind === "denied") return c.json({ error: "Forbidden by policy" }, 403);
  if (outcome.kind === "error") {
    return c.json(
      { error: outcome.signed ? "Invalid or expired signed identity" : "Authorization service unavailable" },
      outcome.signed ? 401 : 503,
    );
  }
  return undefined;
}

export function createApp({ authorize = authorizeAction }: { authorize?: Authorize } = {}) {
  const app = new Hono<{ Bindings: Bindings }>();
  const tasks = createTaskStore();

  app.use("*", async (c: Context<{ Bindings: Bindings }>, next: Next) => {
    const origin = c.req.header("origin");
    const allowedOrigin = environment(c).frontendOrigin;
    if (c.req.method === "OPTIONS" && origin !== allowedOrigin) {
      return c.json({ error: "Origin not allowed" }, 403);
    }
    await next();
    c.header("Vary", "Origin");
    if (origin === allowedOrigin) {
      c.header("Access-Control-Allow-Origin", origin);
      c.header("Access-Control-Allow-Headers", "Authorization, Content-Type, x-user-id");
      c.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }
  });
  app.options("*", (c) => c.body(null, 204));

  async function check(
    c: Context<{ Bindings: Bindings }>,
    action: TaskAction,
    resource: AuthorizationInput["resource"],
    caller: Identity | undefined = identity(c),
  ) {
    if (!caller) return { response: c.json({ error: "A known x-user-id and valid Bearer syntax are required" }, 401) };
    // This is the single enforcement seam shared by all three Hono adapters.
    const outcome = await authorize({
      action,
      issuer: environment(c).issuer,
      resource,
      ...caller,
    });
    return { caller, response: authorizationResponse(c, outcome) };
  }

  app.get("/tasks", async (c) => {
    const caller = identity(c);
    if (!caller) return c.json({ error: "A known x-user-id is required" }, 401);
    const checked = await check(c, "ViewTask", collectionResource(caller.userId), caller);
    return checked.response ?? c.json(tasks.all());
  });

  app.post("/tasks", async (c) => {
    const body = await jsonBody(c);
    if (!body || Object.keys(body).some((key) => key !== "title") || !validTitle(body.title)) {
      return c.json({ error: "title must be 1-120 characters" }, 400);
    }
    const caller = identity(c);
    if (!caller) return c.json({ error: "A known x-user-id is required" }, 401);
    const checked = await check(c, "CreateTask", newTaskResource(caller.userId, body.title.trim()), caller);
    return checked.response ?? c.json(tasks.create(body.title.trim(), caller.userId), 201);
  });

  app.put("/tasks/:id", async (c) => {
    const task = tasks.find(c.req.param("id"));
    if (!task) return c.json({ error: "Task not found" }, 404);
    const body = await jsonBody(c);
    const keys = body ? Object.keys(body) : [];
    if (
      !body ||
      keys.length === 0 ||
      keys.some((key) => !["title", "completed"].includes(key)) ||
      ("title" in body && !validTitle(body.title)) ||
      ("completed" in body && typeof body.completed !== "boolean")
    ) {
      return c.json({ error: "update accepts a valid title and/or boolean completed" }, 400);
    }
    const checked = await check(c, "UpdateTask", taskResource(task));
    if (checked.response) return checked.response;
    return c.json(tasks.update(task.id, {
      ...(typeof body.title === "string" ? { title: body.title.trim() } : {}),
      ...(typeof body.completed === "boolean" ? { completed: body.completed } : {}),
    }));
  });

  app.delete("/tasks/:id", async (c) => {
    const task = tasks.find(c.req.param("id"));
    if (!task) return c.json({ error: "Task not found" }, 404);
    const checked = await check(c, "DeleteTask", taskResource(task));
    if (checked.response) return checked.response;
    tasks.remove(task.id);
    return c.body(null, 204);
  });
  return app;
}

export default createApp();
