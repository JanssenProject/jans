import type { CedarEntity } from "@janssenproject/cedarling";

import type { Task } from "../tasks";
import { getCedarling } from "./init";

export type TaskAction = "CreateTask" | "ViewTask" | "UpdateTask" | "DeleteTask";
export type AuthorizationOutcome =
  | { kind: "allowed" }
  | { kind: "denied" }
  | { kind: "error"; signed: boolean };

export interface AuthorizationInput {
  action: TaskAction;
  issuer: string;
  resource: CedarEntity;
  token?: string;
  userId: string;
}

export async function authorizeAction(input: AuthorizationInput): Promise<AuthorizationOutcome> {
  try {
    const client = await getCedarling(input.issuer);
    // A Bearer token exercises signature validation and token mapping; without
    // one, the application explicitly provides the principal and context.
    const result = input.token
      ? await client.authorizeMultiIssuer({
          tokens: [{ mapping: "LocalMockIdP::Userinfo_token", payload: input.token }],
          action: `TaskApp::Action::"${input.action}"`,
          resource: input.resource,
          context: {},
        })
      : await client.authorizeUnsigned({
          principal: { type: "TaskApp::User", id: input.userId },
          action: `TaskApp::Action::"${input.action}"`,
          resource: input.resource,
          context: { userId: input.userId },
        });
    // Keep evaluation failures distinct from policy denials so the HTTP layer
    // can fail closed with an appropriate status.
    if (!result.ok) return { kind: "error", signed: Boolean(input.token) };
    return result.value.decision ? { kind: "allowed" } : { kind: "denied" };
  } catch {
    return { kind: "error", signed: Boolean(input.token) };
  }
}

// Request handlers construct these resources from server-owned task state
// instead of accepting Cedar attributes from the caller.
function resource(id: string, owner: string, title: string, completed: boolean): CedarEntity {
  return { type: "TaskApp::Task", id, attributes: { owner, title, completed } };
}

export const taskResource = (task: Task) =>
  resource(task.id, task.owner, task.title, task.completed);
export const collectionResource = (userId: string) =>
  resource("task-collection", userId, "Tasks", false);
export const newTaskResource = (userId: string, title: string) =>
  resource("new-task", userId, title, false);
