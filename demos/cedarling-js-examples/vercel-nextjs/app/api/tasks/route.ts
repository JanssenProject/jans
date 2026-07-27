import { NextRequest, NextResponse } from 'next/server';
import { getAll, create } from '@/libs/tasks';
import { authorizeAction, buildResource } from '@/libs/cedarling/authorize';
import { resolveRequestIdentity } from '@/libs/oidc/auth';

export async function GET(req: NextRequest) {
  const identity = await resolveRequestIdentity(req);
  if (!identity) return NextResponse.json({ error: 'Authentication required' }, { status: 401 });

  const authz = await authorizeAction('ViewTask', identity.userId, buildResource(null), identity.token);
  if (!authz.allowed) return NextResponse.json({ error: 'Forbidden' }, { status: 403 });

  return NextResponse.json(getAll());
}

export async function POST(req: NextRequest) {
  const identity = await resolveRequestIdentity(req);
  if (!identity) return NextResponse.json({ error: 'Authentication required' }, { status: 401 });
  const body = await req.json();

  const authz = await authorizeAction('CreateTask', identity.userId, buildResource(null, body.title), identity.token);
  if (!authz.allowed) return NextResponse.json({ error: 'Forbidden' }, { status: 403 });

  const task = create(body.title, identity.userId);
  return NextResponse.json(task, { status: 201 });
}
