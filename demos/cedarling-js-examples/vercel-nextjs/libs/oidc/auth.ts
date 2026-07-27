import type { NextRequest } from 'next/server';
import { getDiscovery } from './provider';
import { getSessionValues } from './session';
import { verifyUserinfoToken } from './verify';

export interface RequestIdentity {
  readonly userId: string;
  readonly token?: string;
}

export async function resolveRequestIdentity(
  request: NextRequest,
): Promise<RequestIdentity | null> {
  const authorization = request.headers.get('authorization') ?? '';
  if (authorization.startsWith('Bearer ')) {
    const token = authorization.slice(7);
    const userId = request.headers.get('x-user-id');
    if (!token || !userId) return null;
    return { userId, token };
  }

  if (request.cookies.get('authMode')?.value !== 'signed-idp') {
    return { userId: request.headers.get('x-user-id') || 'bob' };
  }

  const { clientId, userinfoToken } = getSessionValues(request);
  if (!clientId || !userinfoToken) return null;
  try {
    const claims = await verifyUserinfoToken(
      userinfoToken,
      await getDiscovery(),
      clientId,
    );
    if (typeof claims.sub !== 'string') return null;
    return {
      userId: claims.sub,
      token: userinfoToken,
    };
  } catch (error) {
    console.error('[oidc] Refusing invalid signed session', error);
    return null;
  }
}
