import type { CedarEntity } from "@janssenproject/cedarling";

import type { TaskAction } from "../../shared/contracts";
import { getCedarling } from "./init";

export type AuthorizationOutcome = "allowed" | "denied" | "error";

export async function authorizeAction(
  action: TaskAction,
  userId: string,
  resource: CedarEntity,
  token?: string,
): Promise<AuthorizationOutcome> {
  try {
    const client = await getCedarling();
    // Main uses token mapping for its private signed session and an explicit
    // principal only when the application deliberately operates unsigned.
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
    // Keep SDK failures distinct from policy denials so every IPC handler can
    // fail closed.
    if (!result.ok) return "error";
    return result.value.decision ? "allowed" : "denied";
  } catch {
    return "error";
  }
}
