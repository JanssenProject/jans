import type { CedarEntity } from "@janssenproject/cedarling";

import type { Task } from "../demo-domain";
import { getCedarling } from "./init";

export type TaskAction = "CreateTask" | "ViewTask" | "UpdateTask" | "DeleteTask";
export type AuthorizationOutcome =
  | { kind: "allowed" }
  | { kind: "denied" }
  | { kind: "error"; signed: boolean };

export function authorizationFailure(outcome: AuthorizationOutcome) {
  if (outcome.kind === "allowed") return undefined;
  if (outcome.kind === "denied") return { status: 403, error: "Forbidden by policy" } as const;
  return outcome.signed
    ? ({ status: 401, error: "Invalid or expired signed identity" } as const)
    : ({ status: 503, error: "Authorization service unavailable" } as const);
}

export async function authorizeAction(
  action: TaskAction,
  userId: string,
  resource: CedarEntity,
  token?: string,
): Promise<AuthorizationOutcome> {
  try {
    const client = await getCedarling();
    // Signed sessions exercise UserInfo signature validation and token mapping;
    // unsigned requests deliberately supply an application principal.
    const result = token
      ? await client.authorizeMultiIssuer({
          tokens: [{ mapping: "LocalMockIdP::Userinfo_token", payload: token }],
          action: `TaskApp::Action::"${action}"`,
          resource,
          context: {},
        })
      : await client.authorizeUnsigned({
          principal: { type: "TaskApp::User", id: userId },
          action: `TaskApp::Action::"${action}"`,
          resource,
          context: { userId },
        });
    // Preserve the difference between an evaluation error and a policy deny so
    // routes can fail closed without reporting every failure as HTTP 403.
    if (!result.ok) return { kind: "error", signed: Boolean(token) };
    return result.value.decision ? { kind: "allowed" } : { kind: "denied" };
  } catch {
    return { kind: "error", signed: Boolean(token) };
  }
}

// Only server routes call these helpers, so Cedar attributes are derived from
// server-owned task state rather than arbitrary request JSON.
function resource(id: string, owner: string, title: string, completed: boolean): CedarEntity {
  return { type: "TaskApp::Task", id, attributes: { owner, title, completed } };
}

export const taskResource = (task: Task) => resource(task.id, task.owner, task.title, task.completed);
export const collectionResource = (userId: string) => resource("task-collection", userId, "Tasks", false);
export const newTaskResource = (userId: string, title: string) => resource("new-task", userId, title, false);
