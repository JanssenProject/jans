import { NextRequest, NextResponse } from 'next/server';
import { authorizeAction } from '@/libs/cedarling/authorize';

export const runtime = 'edge';

export async function GET(request: NextRequest) {
  const { searchParams } = request.nextUrl;
  const action = searchParams.get('action') ?? '';
  const userId = searchParams.get('userId') ?? 'bob';
  const taskId = searchParams.get('taskId') ?? '';
  const owner = searchParams.get('owner') ?? '';
  const title = searchParams.get('title') ?? 'untitled';
  const completed = searchParams.get('completed') === 'true';
  const token = searchParams.get('token') ?? undefined;

  const authorization = await authorizeAction(
    action,
    userId,
    {
      type: 'TaskApp::Task',
      id: taskId || 'list-tasks',
      attributes: { owner, title, completed },
    },
    token,
  );

  return NextResponse.json({
    allowed: authorization.allowed,
    runtime,
  });
}
