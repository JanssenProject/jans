import { NextRequest, NextResponse } from 'next/server';
import { authorizeAction } from '@/libs/cedarling/authorize';
import { resolveRequestIdentity } from '@/libs/oidc/auth';

export async function GET(req: NextRequest) {
  const identity = await resolveRequestIdentity(req);
  if (!identity) return NextResponse.json({ error: 'Authentication required' }, { status: 401 });

  const { searchParams } = new URL(req.url);
  const action = searchParams.get('action') || '';
  const taskId = searchParams.get('taskId') || '';
  const owner = searchParams.get('owner') || '';
  const title = searchParams.get('title') || 'untitled';
  const completed = searchParams.get('completed') === 'true';

  const authz = await authorizeAction(
    action,
    identity.userId,
    {
      type: 'TaskApp::Task',
      id: taskId || 'list-tasks',
      attributes: { owner, title, completed },
    },
    identity.token,
  );

  return NextResponse.json({ allowed: authz.allowed });
}
