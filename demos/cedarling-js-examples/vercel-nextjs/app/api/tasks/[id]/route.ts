import { NextRequest, NextResponse } from 'next/server';
import { findById, update, remove } from '@/libs/tasks';
import { authorizeAction, buildResource } from '@/libs/cedarling/authorize';
import { resolveRequestIdentity } from '@/libs/oidc/auth';

export async function PUT(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const identity = await resolveRequestIdentity(req);
  if (!identity) return NextResponse.json({ error: 'Authentication required' }, { status: 401 });
  const body = await req.json();

  const task = findById(id);
  if (!task) return NextResponse.json({ error: 'Task not found' }, { status: 404 });

  const authz = await authorizeAction('UpdateTask', identity.userId, buildResource(task), identity.token);
  if (!authz.allowed) return NextResponse.json({ error: 'Forbidden' }, { status: 403 });

  const updated = update(id, body);
  return NextResponse.json(updated);
}

export async function DELETE(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const identity = await resolveRequestIdentity(req);
  if (!identity) return NextResponse.json({ error: 'Authentication required' }, { status: 401 });

  const task = findById(id);
  if (!task) return NextResponse.json({ error: 'Task not found' }, { status: 404 });

  const authz = await authorizeAction('DeleteTask', identity.userId, buildResource(task), identity.token);
  if (!authz.allowed) return NextResponse.json({ error: 'Forbidden' }, { status: 403 });

  remove(id);
  return new NextResponse(null, { status: 204 });
}
