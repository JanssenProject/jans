import type { CedarlingClient } from '@janssenproject/cedarling';

export type PermissionMap = Record<
  string,
  { readonly canUpdate: boolean; readonly canDelete: boolean }
>;

export interface Task {
  readonly id: string;
  readonly title: string;
  readonly completed: boolean;
  readonly owner: string;
}

export async function checkUnsignedPermissions(
  cedarling: CedarlingClient,
  currentUser: string,
  tasks: readonly Task[],
): Promise<PermissionMap> {
  const permissions = await Promise.all(
    tasks.map(async (task) => {
      const request = {
        principal: { type: 'TaskApp::User', id: currentUser },
        resource: {
          type: 'TaskApp::Task',
          id: task.id,
          attributes: {
            owner: task.owner,
            title: task.title,
            completed: task.completed,
          },
        },
        context: { userId: currentUser },
      };
      const [update, remove] = await Promise.all([
        cedarling.authorizeUnsigned({
          ...request,
          action: 'TaskApp::Action::"UpdateTask"',
        }),
        cedarling.authorizeUnsigned({
          ...request,
          action: 'TaskApp::Action::"DeleteTask"',
        }),
      ]);
      return [
        task.id,
        {
          canUpdate: update.allowed,
          canDelete: remove.allowed,
        },
      ] as const;
    }),
  );

  return Object.fromEntries(permissions);
}
