export type PermMap = Record<string, { canUpdate: boolean; canDelete: boolean }>;

type Task = {
  id: string;
  title: string;
  completed: boolean;
  owner: string;
};

function truncateToken(token: string): string {
  if (!token || token.length <= 20) return token || '(none)';
  return token.substring(0, 12) + '...' + token.substring(token.length - 8);
}

export async function checkSignedPermissions(
  cedarling: any,
  currentUser: string,
  tasks: Task[],
  token: string,
): Promise<PermMap> {
  const perms: PermMap = {};

  for (const task of tasks) {
    const req = {
      tokens: [{ mapping: 'LocalMockIdP::Userinfo_token', payload: token }],
      resource: {
        type: 'TaskApp::Task',
        id: task.id,
        attributes: { owner: task.owner, title: task.title, completed: task.completed },
      },
      context: {},
    };

    console.log(`[authz] signed: user=${currentUser} task=${task.id} token=${truncateToken(token)}`);

    const updateAction = { ...req, action: 'TaskApp::Action::"UpdateTask"' };
    const updateRes = await cedarling.authorizeMultiIssuer(updateAction);
    if (!updateRes.ok) {
      console.log(`[authz] signed: UpdateTask ERROR code=${updateRes.error.code} msg=${updateRes.error.message}`);
    } else {
      console.log(`[authz] signed: UpdateTask decision=${updateRes.value.decision} requestId=${updateRes.value.requestId}`);
    }

    const deleteAction = { ...req, action: 'TaskApp::Action::"DeleteTask"' };
    const deleteRes = await cedarling.authorizeMultiIssuer(deleteAction);
    if (!deleteRes.ok) {
      console.log(`[authz] signed: DeleteTask ERROR code=${deleteRes.error.code} msg=${deleteRes.error.message}`);
    }

    perms[task.id] = {
      canUpdate: updateRes.ok ? updateRes.value.decision : false,
      canDelete: deleteRes.ok ? deleteRes.value.decision : false,
    };
  }

  return perms;
}
