import { NextRequest, NextResponse } from 'next/server';
import { isUserId } from '@/libs/demo-domain';
import { getRegisteredClient, getRequestOrigin } from '@/libs/oidc/provider';
import {
  createPkceChallenge,
  randomBase64Url,
  setTransactionCookies,
} from '@/libs/oidc/session';

export async function GET(request: NextRequest) {
  try {
    const username = request.nextUrl.searchParams.get('user') ?? '';
    if (!isUserId(username)) {
      return NextResponse.json({ error: 'Unknown demo user' }, { status: 400 });
    }

    const origin = getRequestOrigin(request);
    const client = await getRegisteredClient(origin);
    const state = randomBase64Url();
    const nonce = randomBase64Url();
    const verifier = randomBase64Url();
    const challenge = await createPkceChallenge(verifier);
    const authorizationUrl = new URL(client.discovery.authorization_endpoint);
    authorizationUrl.search = new URLSearchParams({
      client_id: client.clientId,
      response_type: 'code',
      redirect_uri: client.redirectUri,
      scope: 'openid profile role',
      resource: client.discovery.issuer,
      state,
      nonce,
      code_challenge: challenge,
      code_challenge_method: 'S256',
      login_hint: username,
      prompt: 'login',
    }).toString();

    const response = NextResponse.redirect(authorizationUrl, 307);
    setTransactionCookies(response, request, {
      state,
      verifier,
      nonce,
      clientId: client.clientId,
    });
    return response;
  } catch (error) {
    console.error('[oidc] Failed to start authorization', error);
    return NextResponse.json(
      { error: 'Unable to start OIDC authorization' },
      { status: 502 },
    );
  }
}
