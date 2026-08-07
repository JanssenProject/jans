import { NextRequest, NextResponse } from "next/server";

import {
  authorizationFailure,
  authorizeAction,
  taskResource,
  type TaskAction,
} from "./cedarling/authorize";
import { resolveRequestIdentity } from "./oidc/auth";
import { findById } from "./tasks";

const PERMISSION_ACTIONS = new Set<TaskAction>(["UpdateTask", "DeleteTask"]);

// This endpoint returns a UI preview. The task mutation routes repeat the
// Cedarling check and remain the actual policy enforcement points.
export async function handlePermissionCheck(
  request: NextRequest,
  runtime?: "edge",
) {
  const identity = await resolveRequestIdentity(request);
  if (!identity) {
    return NextResponse.json({ error: "Authentication required" }, { status: 401 });
  }
  const action = request.nextUrl.searchParams.get("action") as TaskAction | null;
  const task = findById(request.nextUrl.searchParams.get("taskId") ?? "");
  if (!action || !PERMISSION_ACTIONS.has(action)) {
    return NextResponse.json(
      { error: "action must be UpdateTask or DeleteTask" },
      { status: 400 },
    );
  }
  if (!task) return NextResponse.json({ error: "Task not found" }, { status: 404 });

  // Importing route metadata determines whether this runs in Node or Edge.
  const outcome = await authorizeAction(action, identity.userId, taskResource(task), identity.token);
  const failure = authorizationFailure(outcome);
  if (failure && failure.status !== 403) {
    return NextResponse.json({ error: failure.error }, { status: failure.status });
  }
  return NextResponse.json({ allowed: outcome.kind === "allowed", ...(runtime ? { runtime } : {}) });
}
