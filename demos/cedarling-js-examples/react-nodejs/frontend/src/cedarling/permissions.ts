import type { CedarEntity, CedarlingClient } from "@janssenproject/cedarling";

import type { PermissionMap, Task, UserId } from "../model";

function taskResource(task: Task): CedarEntity {
  return {
    type: "TaskApp::Task",
    id: task.id,
    attributes: {
      owner: task.owner,
      title: task.title,
      completed: task.completed,
    },
  };
}

export async function checkPermissions(
  client: CedarlingClient,
  userId: UserId,
  tasks: readonly Task[],
  userinfoToken?: string,
): Promise<PermissionMap> {
  // These decisions only drive the UI. The Express backend repeats every check
  // with server-owned resources before it mutates data.
  const entries = await Promise.all(
    tasks.map(async (task) => {
      const authorize = (action: "UpdateTask" | "DeleteTask") =>
        // A signed UserInfo JWT exercises token mapping and signature checks;
        // otherwise the application deliberately supplies an unsigned user.
        userinfoToken
          ? client.authorizeMultiIssuer({
              tokens: [
                {
                  mapping: "LocalMockIdP::Userinfo_token",
                  payload: userinfoToken,
                },
              ],
              action: `TaskApp::Action::"${action}"`,
              resource: taskResource(task),
              context: {},
            })
          : client.authorizeUnsigned({
              principal: { type: "TaskApp::User", id: userId },
              action: `TaskApp::Action::"${action}"`,
              resource: taskResource(task),
              context: { userId },
            });
      const [update, remove] = await Promise.all([
        authorize("UpdateTask"),
        authorize("DeleteTask"),
      ]);
      return [
        task.id,
        {
          // Result errors fail closed just like explicit deny decisions.
          canUpdate: update.ok && update.value.decision,
          canDelete: remove.ok && remove.value.decision,
        },
      ] as const;
    }),
  );
  return Object.fromEntries(entries);
}
