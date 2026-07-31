import { NextRequest, NextResponse } from "next/server";

import { authorizationFailure, authorizeAction, taskResource } from "@/libs/cedarling/authorize";
import { isValidTaskTitle } from "@/libs/demo-domain";
import { resolveRequestIdentity } from "@/libs/oidc/auth";
import { findById, remove, update } from "@/libs/tasks";

type RouteContext = { params: Promise<{ id: string }> };

export async function PUT(request: NextRequest, { params }: RouteContext) {
  const task = findById((await params).id);
  if (!task) return NextResponse.json({ error: "Task not found" }, { status: 404 });
  const identity = await resolveRequestIdentity(request);
  if (!identity) return NextResponse.json({ error: "Authentication required" }, { status: 401 });
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Request body must be valid JSON" }, { status: 400 });
  }
  const record = body && typeof body === "object" && !Array.isArray(body)
    ? (body as Record<string, unknown>)
    : undefined;
  const keys = record ? Object.keys(record) : [];
  if (
    !record ||
    keys.length === 0 ||
    keys.some((key) => !["title", "completed"].includes(key)) ||
    ("title" in record && !isValidTaskTitle(record.title)) ||
    ("completed" in record && typeof record.completed !== "boolean")
  ) {
    return NextResponse.json(
      { error: "update accepts a valid title and/or boolean completed" },
      { status: 400 },
    );
  }
  // Cedarling receives the stored task attributes before any requested update
  // is applied.
  const failure = authorizationFailure(
    await authorizeAction("UpdateTask", identity.userId, taskResource(task), identity.token),
  );
  if (failure) return NextResponse.json({ error: failure.error }, { status: failure.status });
  return NextResponse.json(update(task.id, {
    ...(typeof record.title === "string" ? { title: record.title.trim() } : {}),
    ...(typeof record.completed === "boolean" ? { completed: record.completed } : {}),
  }));
}

export async function DELETE(request: NextRequest, { params }: RouteContext) {
  const task = findById((await params).id);
  if (!task) return NextResponse.json({ error: "Task not found" }, { status: 404 });
  const identity = await resolveRequestIdentity(request);
  if (!identity) return NextResponse.json({ error: "Authentication required" }, { status: 401 });
  // Deletion also rechecks policy at the server boundary.
  const failure = authorizationFailure(
    await authorizeAction("DeleteTask", identity.userId, taskResource(task), identity.token),
  );
  if (failure) return NextResponse.json({ error: failure.error }, { status: failure.status });
  remove(task.id);
  return new NextResponse(null, { status: 204 });
}
