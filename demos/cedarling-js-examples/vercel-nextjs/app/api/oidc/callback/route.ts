import { NextRequest, NextResponse } from 'next/server';
import { getDiscovery, getRequestOrigin } from '@/libs/oidc/provider';
import {
  clearTransactionCookies,
  OIDC_COOKIES,
  setSessionCookies,
} from '@/libs/oidc/session';
import { verifyIdToken, verifyUserinfoToken } from '@/libs/oidc/verify';

interface TokenResponse {
  readonly access_token?: unknown;
  readonly id_token?: unknown;
  readonly token_type?: unknown;
  readonly error?: unknown;
  readonly error_description?: unknown;
}

function failedCallback(request: NextRequest, message: string) {
  console.error(`[oidc] Callback failed: ${message}`);
  const origin = getRequestOrigin(request);
  const destination = new URL('/', origin);
  destination.searchParams.set('oidc_error', 'authentication_failed');
  const response = NextResponse.redirect(destination, 303);
  clearTransactionCookies(response, request);
  return response;
}

export async function GET(request: NextRequest) {
  try {
    const providerError = request.nextUrl.searchParams.get('error');
    if (providerError) {
      return failedCallback(request, `provider returned ${providerError}`);
    }

    const code = request.nextUrl.searchParams.get('code');
    const returnedState = request.nextUrl.searchParams.get('state');
    const expectedState = request.cookies.get(OIDC_COOKIES.state)?.value;
    const verifier = request.cookies.get(OIDC_COOKIES.verifier)?.value;
    const nonce = request.cookies.get(OIDC_COOKIES.nonce)?.value;
    const clientId = request.cookies.get(OIDC_COOKIES.clientId)?.value;
    if (!code || !returnedState || !expectedState || returnedState !== expectedState) {
      return failedCallback(request, 'missing or invalid state');
    }
    if (!verifier || !nonce || !clientId) {
      return failedCallback(request, 'OIDC transaction cookie is missing');
    }

    const origin = getRequestOrigin(request);
    const discovery = await getDiscovery();
    const redirectUri = `${origin}/api/oidc/callback`;
    const tokenResponse = await fetch(discovery.token_endpoint, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        code,
        client_id: clientId,
        redirect_uri: redirectUri,
        code_verifier: verifier,
      }),
      cache: 'no-store',
    });
    const tokens = (await tokenResponse.json()) as TokenResponse;
    if (
      !tokenResponse.ok ||
      typeof tokens.access_token !== 'string' ||
      typeof tokens.id_token !== 'string' ||
      tokens.token_type !== 'Bearer'
    ) {
      const detail =
        typeof tokens.error_description === 'string'
          ? tokens.error_description
          : typeof tokens.error === 'string'
            ? tokens.error
            : `HTTP ${tokenResponse.status}`;
      return failedCallback(request, `token exchange failed: ${detail}`);
    }

    const idClaims = await verifyIdToken(tokens.id_token, discovery, clientId, nonce);
    const userinfoResponse = await fetch(discovery.userinfo_endpoint, {
      headers: {
        Accept: 'application/jwt',
        Authorization: `Bearer ${tokens.access_token}`,
      },
      cache: 'no-store',
    });
    const userinfoToken = await userinfoResponse.text();
    if (!userinfoResponse.ok || userinfoToken.split('.').length !== 3) {
      return failedCallback(
        request,
        `signed userinfo request failed with HTTP ${userinfoResponse.status}`,
      );
    }
    await verifyUserinfoToken(userinfoToken, discovery, clientId, idClaims.sub);

    // Keep the verified signed UserInfo in an HttpOnly server session. Server
    // routes later give this JWT to Cedarling without exposing it to the React
    // page.
    const response = NextResponse.redirect(new URL('/', origin), 303);
    setSessionCookies(response, request, {
      clientId,
      idToken: tokens.id_token,
      userinfoToken,
    });
    clearTransactionCookies(response, request);
    return response;
  } catch (error) {
    return failedCallback(
      request,
      error instanceof Error ? error.message : 'unexpected callback error',
    );
  }
}
