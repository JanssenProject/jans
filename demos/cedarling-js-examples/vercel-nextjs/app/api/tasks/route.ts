import { NextRequest, NextResponse } from "next/server";

import {
  authorizationFailure,
  authorizeAction,
  collectionResource,
  newTaskResource,
} from "@/libs/cedarling/authorize";
import { isValidTaskTitle } from "@/libs/demo-domain";
import { resolveRequestIdentity } from "@/libs/oidc/auth";
import { create, getAll } from "@/libs/tasks";

export async function GET(request: NextRequest) {
  const identity = await resolveRequestIdentity(request);
  if (!identity) return NextResponse.json({ error: "Authentication required" }, { status: 401 });
  // Listing is authorized independently of any browser-side permission
  // preview, using a server-constructed collection resource.
  const failure = authorizationFailure(
    await authorizeAction("ViewTask", identity.userId, collectionResource(identity.userId), identity.token),
  );
  return failure
    ? NextResponse.json({ error: failure.error }, { status: failure.status })
    : NextResponse.json(getAll());
}

export async function POST(request: NextRequest) {
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
  if (
    !record ||
    Object.keys(record).some((key) => key !== "title") ||
    !isValidTaskTitle(record.title)
  ) {
    return NextResponse.json({ error: "title must be 1-120 characters" }, { status: 400 });
  }
  const title = record.title.trim();
  // Authorization happens before creation, and the owner comes from the
  // resolved request identity rather than request JSON.
  const failure = authorizationFailure(
    await authorizeAction("CreateTask", identity.userId, newTaskResource(identity.userId, title), identity.token),
  );
  return failure
    ? NextResponse.json({ error: failure.error }, { status: failure.status })
    : NextResponse.json(create(title, identity.userId), { status: 201 });
}
