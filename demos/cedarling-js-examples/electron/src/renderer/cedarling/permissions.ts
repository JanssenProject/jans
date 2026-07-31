import type { CedarlingClient } from "@janssenproject/cedarling";

import type { PermissionMap, Task, UserId } from "../../shared/contracts";

export async function checkPermissions(
  client: CedarlingClient,
  userId: UserId,
  tasks: readonly Task[],
  signed: boolean,
): Promise<PermissionMap> {
  // These decisions only control button state. Main repeats authorization
  // before every task operation.
  const entries = await Promise.all(
    tasks.map(async (task) => {
      if (signed) {
        // Signed checks cross IPC because main alone owns the verified token.
        const [canUpdate, canDelete] = await Promise.all([
          window.electron.cedarling.signedPermission({ userId, id: task.id, action: "UpdateTask" }),
          window.electron.cedarling.signedPermission({ userId, id: task.id, action: "DeleteTask" }),
        ]);
        return [task.id, { canUpdate, canDelete }] as const;
      }
      // Unsigned mode demonstrates direct browser-style Cedarling calls in the
      // sandboxed renderer.
      const request = {
        principal: { type: "TaskApp::User", id: userId },
        resource: {
          type: "TaskApp::Task",
          id: task.id,
          attributes: { owner: task.owner, title: task.title, completed: task.completed },
        },
        context: { userId },
      };
      const [update, remove] = await Promise.all([
        client.authorizeUnsigned({ ...request, action: 'TaskApp::Action::"UpdateTask"' }),
        client.authorizeUnsigned({ ...request, action: 'TaskApp::Action::"DeleteTask"' }),
      ]);
      return [
        task.id,
        {
          // SDK errors and policy denials both disable the corresponding action.
          canUpdate: update.ok && update.value.decision,
          canDelete: remove.ok && remove.value.decision,
        },
      ] as const;
    }),
  );
  return Object.fromEntries(entries);
}
