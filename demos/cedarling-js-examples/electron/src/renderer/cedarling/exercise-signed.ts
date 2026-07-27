import type { PermissionMap, Task } from './exercise-unsigned';

interface AuthorizationResponse {
  readonly allowed: boolean;
}

export async function checkSignedPermissions(
  currentUser: string,
  tasks: readonly Task[],
): Promise<PermissionMap> {
  const permissions = await Promise.all(
    tasks.map(async (task) => {
      const request = {
        userId: currentUser,
        resource: {
          type: 'TaskApp::Task',
          id: task.id,
          attributes: {
            owner: task.owner,
            title: task.title,
            completed: task.completed,
          },
        },
      };
      const [update, remove] = await Promise.all([
        window.electron.ipcRenderer.invoke<AuthorizationResponse>(
          'authorize-signed',
          { ...request, action: 'UpdateTask' },
        ),
        window.electron.ipcRenderer.invoke<AuthorizationResponse>(
          'authorize-signed',
          { ...request, action: 'DeleteTask' },
        ),
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
