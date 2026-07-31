import type { NextRequest } from "next/server";

import { isUserId, type UserId } from "../demo-domain";
import { getDiscovery } from "./provider";
import { getSessionValues } from "./session";
import { verifyUserinfoToken } from "./verify";

export interface RequestIdentity {
  readonly userId: UserId;
  readonly token?: string;
}

export async function resolveRequestIdentity(
  request: NextRequest,
): Promise<RequestIdentity | null> {
  const session = getSessionValues(request);
  const hasSessionCookie = Object.values(session).some(Boolean);
  // A signed server session takes precedence over x-user-id. Re-verify the
  // UserInfo JWT before passing it to Cedarling so malformed sessions fail
  // before policy evaluation.
  if (hasSessionCookie) {
    if (!session.clientId || !session.idToken || !session.userinfoToken) return null;
    try {
      const claims = await verifyUserinfoToken(
        session.userinfoToken,
        await getDiscovery(),
        session.clientId,
      );
      return isUserId(claims.sub)
        ? { userId: claims.sub, token: session.userinfoToken }
        : null;
    } catch {
      return null;
    }
  }

  const userId = request.headers.get("x-user-id");
  return isUserId(userId) ? { userId } : null;
}
