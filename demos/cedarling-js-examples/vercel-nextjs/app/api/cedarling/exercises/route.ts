import { NextRequest, NextResponse } from 'next/server';
import { runFullSdkExercises } from '@/libs/cedarling/exercises';
import { resolveRequestIdentity } from '@/libs/oidc/auth';

export async function POST(request: NextRequest) {
  const identity = await resolveRequestIdentity(request);
  if (!identity) {
    return NextResponse.json({ error: 'Authentication required' }, { status: 401 });
  }

  try {
    return NextResponse.json(
      await runFullSdkExercises(identity.token),
    );
  } catch (error) {
    console.error('[cedarling] SDK exercises failed', error);
    return NextResponse.json(
      { error: 'Cedarling SDK exercises failed' },
      { status: 500 },
    );
  }
}
