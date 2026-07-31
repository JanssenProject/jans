import express from "express";
import { pathToFileURL } from "node:url";

import { authorizeMiddleware } from "./cedarling/authz-middleware.js";
import { initCedarling, shutDownCedarling } from "./cedarling/init.js";

const DEFAULT_TASKS = [
  { id: "task-1", title: "Buy groceries", completed: false, owner: "bob" },
  {
    id: "task-2",
    title: "Schedule meeting with CEO",
    completed: false,
    owner: "alice",
  },
];
const MAX_TITLE_LENGTH = 120;

function exactOriginCors(frontendOrigin) {
  return (req, res, next) => {
    const origin = req.get("origin");
    res.vary("Origin");
    if (origin === frontendOrigin) {
      res.set("Access-Control-Allow-Origin", origin);
      res.set("Access-Control-Allow-Headers", "Authorization, Content-Type, x-user-id");
      res.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }
    if (req.method === "OPTIONS") {
      return origin === frontendOrigin
        ? res.sendStatus(204)
        : res.status(403).json({ error: "Origin not allowed" });
    }
    next();
  };
}

function validTitle(value) {
  return typeof value === "string" && value.trim().length > 0 && value.trim().length <= MAX_TITLE_LENGTH;
}

function validateCreate(req, res, next) {
  if (
    !req.body ||
    Object.keys(req.body).some((key) => key !== "title") ||
    !validTitle(req.body.title)
  ) {
    return res.status(400).json({ error: "title must be 1-120 characters" });
  }
  req.body.title = req.body.title.trim();
  next();
}

function validateUpdate(req, res, next) {
  const keys = req.body && typeof req.body === "object" ? Object.keys(req.body) : [];
  if (
    keys.length === 0 ||
    keys.some((key) => !["title", "completed"].includes(key)) ||
    ("title" in req.body && !validTitle(req.body.title)) ||
    ("completed" in req.body && typeof req.body.completed !== "boolean")
  ) {
    return res.status(400).json({ error: "update accepts a valid title and/or boolean completed" });
  }
  if ("title" in req.body) req.body.title = req.body.title.trim();
  next();
}

export function createTaskApp({
  cedarling,
  frontendOrigin = "http://localhost:3000",
  initialTasks = DEFAULT_TASKS,
}) {
  const tasks = initialTasks.map((task) => ({ ...task }));
  const app = express();
  app.disable("x-powered-by");
  app.use(exactOriginCors(new URL(frontendOrigin).origin));
  app.use(express.json({ limit: "16kb" }));
  // Bind the shared Cedarling engine once; each route selects its Cedar action
  // through the middleware below.
  const authorize = authorizeMiddleware(cedarling);

  const loadTask = (req, res, next) => {
    req.task = tasks.find((task) => task.id === req.params.id);
    return req.task ? next() : res.status(404).json({ error: "Task not found" });
  };

  app.get("/tasks", authorize("ViewTask"), (_req, res) => res.json(tasks));
  // Validation runs first, Cedarling decides second, and mutation happens only
  // after an allow decision calls next().
  app.post("/tasks", validateCreate, authorize("CreateTask"), (req, res) => {
    const task = {
      id: `task-${Date.now()}`,
      title: req.body.title,
      completed: false,
      owner: req.userId,
    };
    tasks.push(task);
    res.status(201).json(task);
  });
  app.put(
    "/tasks/:id",
    loadTask,
    validateUpdate,
    authorize("UpdateTask"),
    (req, res) => {
      if (req.body.completed !== undefined) req.task.completed = req.body.completed;
      if (req.body.title !== undefined) req.task.title = req.body.title;
      res.json(req.task);
    },
  );
  app.delete(
    "/tasks/:id",
    loadTask,
    authorize("DeleteTask"),
    (req, res) => {
      tasks.splice(tasks.indexOf(req.task), 1);
      res.status(204).send();
    },
  );
  app.use((error, _req, res, next) => {
    if (error instanceof SyntaxError && "body" in error) {
      return res.status(400).json({ error: "Request body must be valid JSON" });
    }
    next(error);
  });
  return app;
}

export async function startServer() {
  const cedarling = await initCedarling();
  const port = Number(process.env.PORT ?? 8080);
  const app = createTaskApp({
    cedarling,
    frontendOrigin: process.env.FRONTEND_ORIGIN ?? "http://localhost:3000",
  });
  const server = app.listen(port, () => {
    console.log(`TaskApp Express backend: http://localhost:${port}`);
  });
  let shuttingDown = false;
  // Stop accepting requests before draining the SDK so no authorization call
  // starts during Cedarling shutdown.
  const shutdown = () => {
    if (shuttingDown) return;
    shuttingDown = true;
    server.close(async () => {
      try {
        await shutDownCedarling();
        process.exitCode = 0;
      } catch (error) {
        console.error("Cedarling shutdown failed", error);
        process.exitCode = 1;
      }
    });
  };
  process.once("SIGINT", shutdown);
  process.once("SIGTERM", shutdown);
  return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  await startServer();
}
