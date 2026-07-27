import type { CedarEntity } from '@janssenproject/cedarling';
import { getCedarling } from './init';

export type AuthorizeResponse = {
  allowed: boolean;
  diagnostics?: unknown;
};

export async function authorizeAction(
  action: string,
  userId: string,
  resource: CedarEntity,
  token?: string,
): Promise<AuthorizeResponse> {
  console.log(
    '[Main] authorizeAction: action=%s userId=%s resource=%s/%s',
    action,
    userId,
    resource.type,
    resource.id,
  );

  const client = await getCedarling();
  const isSigned = !!token;

  try {
    if (isSigned) {
      console.log('[Main] authorizeAction: using authorizeMultiIssuer');
      const authResult = await client.authorizeMultiIssuer({
        tokens: [{ mapping: 'LocalMockIdP::Userinfo_token', payload: token }],
        action: `TaskApp::Action::"${action}"`,
        resource,
        context: {},
      });
      if (!authResult.ok) {
        console.error(
          '[Main] authorizeAction: authorizeMultiIssuer returned error:',
          authResult.error,
        );
        return { allowed: false, diagnostics: authResult.error };
      }
      console.log(
        '[Main] authorizeAction: decision=%s',
        authResult.value.decision,
      );
      return {
        allowed: authResult.value.decision,
        diagnostics: authResult.value.diagnostics,
      };
    }

    console.log('[Main] authorizeAction: using authorizeUnsigned');
    const authResult = await client.authorizeUnsigned({
      principal: { type: 'TaskApp::User', id: userId },
      action: `TaskApp::Action::"${action}"`,
      resource,
      context: { userId },
    });

    if (!authResult.ok) {
      console.error(
        '[Main] authorizeAction: authorizeUnsigned returned error:',
        authResult.error,
      );
      return { allowed: false, diagnostics: authResult.error };
    }
    console.log(
      '[Main] authorizeAction: decision=%s',
      authResult.value.decision,
    );
    return {
      allowed: authResult.value.decision,
      diagnostics: authResult.value.diagnostics,
    };
  } catch (err) {
    console.error('[Main] authorizeAction: exception:', err);
    return { allowed: false, diagnostics: err };
  }
}
