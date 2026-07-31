export const USERS = [
  { id: "bob", label: "Bob", note: "owns Buy groceries" },
  { id: "alice", label: "Alice", note: "owns Schedule meeting with CEO" },
  { id: "charlie", label: "Charlie", note: "guest user" },
] as const;

export type UserId = (typeof USERS)[number]["id"];
const USER_IDS = new Set<string>(USERS.map((user) => user.id));

export function isUserId(value: unknown): value is UserId {
  return typeof value === "string" && USER_IDS.has(value);
}

export interface Task {
  id: string;
  title: string;
  completed: boolean;
  owner: UserId;
}

export function isTask(value: unknown): value is Task {
  if (!value || typeof value !== "object") return false;
  const task = value as Record<string, unknown>;
  return (
    typeof task.id === "string" &&
    typeof task.title === "string" &&
    typeof task.completed === "boolean" &&
    isUserId(task.owner)
  );
}

export type PermissionMap = Record<
  string,
  { canUpdate: boolean; canDelete: boolean }
>;
