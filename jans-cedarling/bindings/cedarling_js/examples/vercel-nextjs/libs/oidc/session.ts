import type { NextRequest, NextResponse } from 'next/server';

export const OIDC_COOKIES = {
  state: 'taskapp_oidc_state',
  verifier: 'taskapp_oidc_verifier',
  nonce: 'taskapp_oidc_nonce',
  clientId: 'taskapp_oidc_client_id',
  idToken: 'taskapp_oidc_id_token',
  userinfoToken: 'taskapp_oidc_userinfo_token',
} as const;

const TRANSACTION_MAX_AGE_SECONDS = 10 * 60;
const SESSION_MAX_AGE_SECONDS = 60 * 60;

export function randomBase64Url(byteLength = 32): string {
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return Buffer.from(bytes).toString('base64url');
}

export async function createPkceChallenge(verifier: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    'SHA-256',
    new TextEncoder().encode(verifier),
  );
  return Buffer.from(digest).toString('base64url');
}

function cookieOptions(request: NextRequest, maxAge: number) {
  return {
    httpOnly: true,
    sameSite: 'lax' as const,
    secure: new URL(request.url).protocol === 'https:',
    path: '/',
    maxAge,
  };
}

export function setTransactionCookies(
  response: NextResponse,
  request: NextRequest,
  values: {
    readonly state: string;
    readonly verifier: string;
    readonly nonce: string;
    readonly clientId: string;
  },
): void {
  const options = cookieOptions(request, TRANSACTION_MAX_AGE_SECONDS);
  response.cookies.set(OIDC_COOKIES.state, values.state, options);
  response.cookies.set(OIDC_COOKIES.verifier, values.verifier, options);
  response.cookies.set(OIDC_COOKIES.nonce, values.nonce, options);
  response.cookies.set(OIDC_COOKIES.clientId, values.clientId, options);
}

export function clearTransactionCookies(
  response: NextResponse,
  request: NextRequest,
): void {
  const options = cookieOptions(request, 0);
  response.cookies.set(OIDC_COOKIES.state, '', options);
  response.cookies.set(OIDC_COOKIES.verifier, '', options);
  response.cookies.set(OIDC_COOKIES.nonce, '', options);
}

export function setSessionCookies(
  response: NextResponse,
  request: NextRequest,
  values: {
    readonly clientId: string;
    readonly idToken: string;
    readonly userinfoToken: string;
  },
): void {
  const options = cookieOptions(request, SESSION_MAX_AGE_SECONDS);
  response.cookies.set(OIDC_COOKIES.clientId, values.clientId, options);
  response.cookies.set(OIDC_COOKIES.idToken, values.idToken, options);
  response.cookies.set(OIDC_COOKIES.userinfoToken, values.userinfoToken, options);
}

export function clearSessionCookies(
  response: NextResponse,
  request: NextRequest,
): void {
  const options = cookieOptions(request, 0);
  response.cookies.set(OIDC_COOKIES.clientId, '', options);
  response.cookies.set(OIDC_COOKIES.idToken, '', options);
  response.cookies.set(OIDC_COOKIES.userinfoToken, '', options);
}

export function setAuthModeCookie(
  response: NextResponse,
  request: NextRequest,
  mode: 'unsigned' | 'signed-idp',
): void {
  response.cookies.set('authMode', mode, {
    httpOnly: false,
    sameSite: 'lax',
    secure: new URL(request.url).protocol === 'https:',
    path: '/',
    maxAge: 24 * 60 * 60,
  });
}

export function getSessionValues(request: NextRequest) {
  return {
    clientId: request.cookies.get(OIDC_COOKIES.clientId)?.value,
    idToken: request.cookies.get(OIDC_COOKIES.idToken)?.value,
    userinfoToken: request.cookies.get(OIDC_COOKIES.userinfoToken)?.value,
  };
}
