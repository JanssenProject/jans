import type {
  AuthorizationDiagnostics,
  CedarEntity,
} from '@janssenproject/cedarling';
import { getCedarling } from './init';
import type { Task } from '../tasks';

export interface AuthorizationOutcome {
  readonly allowed: boolean;
  readonly diagnostics?: AuthorizationDiagnostics;
}

export async function authorizeAction(
  action: string,
  userId: string,
  resource: CedarEntity,
  token?: string,
): Promise<AuthorizationOutcome> {
  const client = await getCedarling();
  const authResult = token
    ? await client.authorizeMultiIssuer({
        tokens: [{ mapping: 'LocalMockIdP::Userinfo_token', payload: token }],
        action: `TaskApp::Action::"${action}"`,
        resource,
        context: {},
      })
    : await client.authorizeUnsigned({
        principal: { type: 'TaskApp::User', id: userId },
        action: `TaskApp::Action::"${action}"`,
        resource,
        context: { userId },
      });

  if (!authResult.ok) {
    console.error(
      `[authz] ERROR user="${userId}" action="${action}":`,
      authResult.error,
    );
    return { allowed: false };
  }

  return {
    allowed: authResult.value.decision,
    diagnostics: authResult.value.diagnostics,
  };
}

export function buildResource(task: Task | null, bodyTitle?: string): CedarEntity {
  return {
    type: 'TaskApp::Task',
    id: task?.id || 'list-tasks',
    attributes: {
      owner: task?.owner || '',
      title: task?.title || bodyTitle || 'untitled',
      completed: task?.completed ?? false,
    },
  };
}
