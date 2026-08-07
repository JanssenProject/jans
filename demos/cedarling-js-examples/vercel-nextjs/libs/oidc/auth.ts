import type { NextRequest } from "next/server";

import { isUserId, type UserId } from "../demo-domain";
import { getDiscovery } from "./provider";
import { getSessionValues } from "./session";
import { verifyIdTokenForSession, verifyUserinfoToken } from "./verify";

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
      const discovery = await getDiscovery();
      // The UserInfo token must agree with the stored ID token on both audience
      // client and subject, matching the validation done at login, so cookie
      // values cannot be mixed across sessions or clients.
      const idClaims = await verifyIdTokenForSession(
        session.idToken,
        discovery,
        session.clientId,
      );
      const claims = await verifyUserinfoToken(
        session.userinfoToken,
        discovery,
        session.clientId,
        idClaims.sub,
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
