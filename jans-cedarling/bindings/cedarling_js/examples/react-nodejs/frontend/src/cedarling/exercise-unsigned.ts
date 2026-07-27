export type PermMap = Record<string, { canUpdate: boolean; canDelete: boolean }>;

type Task = {
  id: string;
  title: string;
  completed: boolean;
  owner: string;
};

export async function checkUnsignedPermissions(
  cedarling: any,
  currentUser: string,
  tasks: Task[],
): Promise<PermMap> {
  const perms: PermMap = {};

  for (const task of tasks) {
    const updateRes = await cedarling.authorizeUnsigned({
      principal: { type: 'TaskApp::User', id: currentUser },
      action: 'TaskApp::Action::"UpdateTask"',
      resource: {
        type: 'TaskApp::Task',
        id: task.id,
        attributes: { owner: task.owner, title: task.title, completed: task.completed },
      },
      context: { userId: currentUser },
    });

    const deleteRes = await cedarling.authorizeUnsigned({
      principal: { type: 'TaskApp::User', id: currentUser },
      action: 'TaskApp::Action::"DeleteTask"',
      resource: {
        type: 'TaskApp::Task',
        id: task.id,
        attributes: { owner: task.owner, title: task.title, completed: task.completed },
      },
      context: { userId: currentUser },
    });

    perms[task.id] = {
      canUpdate: updateRes.ok ? updateRes.value.decision : false,
      canDelete: deleteRes.ok ? deleteRes.value.decision : false,
    };
  }

  return perms;
}
