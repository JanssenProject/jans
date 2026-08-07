import { NextRequest, NextResponse } from 'next/server';
import { isUserId } from '@/libs/demo-domain';
import { getDiscovery } from '@/libs/oidc/provider';
import { clearSessionCookies, getSessionValues } from '@/libs/oidc/session';
import { verifyUserinfoToken } from '@/libs/oidc/verify';

export async function GET(request: NextRequest) {
  const { clientId, userinfoToken } = getSessionValues(request);
  if (!clientId || !userinfoToken) {
    return NextResponse.json({ authenticated: false });
  }

  try {
    const claims = await verifyUserinfoToken(
      userinfoToken,
      await getDiscovery(),
      clientId,
    );
    if (!isUserId(claims.sub)) {
      throw new Error("Signed UserInfo identifies an unknown demo user");
    }
    return NextResponse.json({
      authenticated: true,
      userId: claims.sub,
    });
  } catch (error) {
    console.error('[oidc] Invalid session cookie', error);
    const response = NextResponse.json({ authenticated: false });
    clearSessionCookies(response, request);
    return response;
  }
}
