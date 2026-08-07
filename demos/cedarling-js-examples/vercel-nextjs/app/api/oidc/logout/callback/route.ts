import { NextRequest, NextResponse } from 'next/server';
import { getRequestOrigin } from '@/libs/oidc/provider';
import { clearSessionCookies } from '@/libs/oidc/session';

export async function GET(request: NextRequest) {
  const response = NextResponse.redirect(
    new URL('/', getRequestOrigin(request)),
    303,
  );
  clearSessionCookies(response, request);
  return response;
}
