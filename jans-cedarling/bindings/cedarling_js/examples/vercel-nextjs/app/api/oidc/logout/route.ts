import { NextRequest, NextResponse } from 'next/server';
import { getDiscovery, getRequestOrigin } from '@/libs/oidc/provider';
import {
  clearSessionCookies,
  getSessionValues,
  setAuthModeCookie,
} from '@/libs/oidc/session';

export async function GET(request: NextRequest) {
  const origin = getRequestOrigin(request);
  const { clientId, idToken } = getSessionValues(request);
  let destination = new URL('/', origin);

  if (clientId && idToken) {
    try {
      const discovery = await getDiscovery();
      if (discovery.end_session_endpoint) {
        destination = new URL(discovery.end_session_endpoint);
        destination.search = new URLSearchParams({
          client_id: clientId,
          id_token_hint: idToken,
          post_logout_redirect_uri: `${origin}/api/oidc/logout/callback`,
        }).toString();
      }
    } catch (error) {
      console.error('[oidc] Unable to contact provider during logout', error);
    }
  }

  const response = NextResponse.redirect(destination, 303);
  clearSessionCookies(response, request);
  setAuthModeCookie(response, request, 'signed-idp');
  return response;
}
