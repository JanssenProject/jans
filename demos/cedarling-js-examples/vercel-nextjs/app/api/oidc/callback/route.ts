import { NextRequest, NextResponse } from 'next/server';
import { getDiscovery, getRequestOrigin } from '@/libs/oidc/provider';
import {
  clearTransactionCookies,
  OIDC_COOKIES,
  setSessionCookies,
} from '@/libs/oidc/session';
import { verifyIdToken, verifyUserinfoToken } from '@/libs/oidc/verify';
import {
  callbackFailureLog,
  type CallbackFailureReason,
  validateCallbackParameters,
} from '@/libs/oidc/callback';

interface TokenResponse {
  readonly access_token?: unknown;
  readonly id_token?: unknown;
  readonly token_type?: unknown;
}

function failedCallback(request: NextRequest, reason: CallbackFailureReason) {
  console.error(callbackFailureLog(reason));
  const origin = getRequestOrigin(request);
  const destination = new URL('/', origin);
  destination.searchParams.set('oidc_error', 'authentication_failed');
  const response = NextResponse.redirect(destination, 303);
  clearTransactionCookies(response, request);
  return response;
}

export async function GET(request: NextRequest) {
  try {
    const expectedState = request.cookies.get(OIDC_COOKIES.state)?.value;
    const verifier = request.cookies.get(OIDC_COOKIES.verifier)?.value;
    const nonce = request.cookies.get(OIDC_COOKIES.nonce)?.value;
    const clientId = request.cookies.get(OIDC_COOKIES.clientId)?.value;
    if (!expectedState || !verifier || !nonce || !clientId) {
      return failedCallback(request, 'missing_transaction');
    }
    const callback = validateCallbackParameters(
      request.nextUrl.searchParams,
      expectedState,
    );
    if (!callback.ok) {
      return failedCallback(request, callback.reason);
    }
    const { code } = callback;

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
      return failedCallback(request, 'token_exchange_failed');
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
      return failedCallback(request, 'userinfo_failed');
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
  } catch {
    return failedCallback(request, 'unexpected_error');
  }
}
