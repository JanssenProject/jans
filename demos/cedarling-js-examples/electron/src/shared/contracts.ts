import type { PolicyStoreDocument } from "@janssenproject/cedarling";

export const USERS = [
  { id: "bob", label: "Bob", note: "owns Buy groceries" },
  { id: "alice", label: "Alice", note: "owns Schedule meeting with CEO" },
  { id: "charlie", label: "Charlie", note: "guest user" },
] as const;

export const MAX_TITLE_LENGTH = 120;
export type UserId = (typeof USERS)[number]["id"];
export type TaskAction = "CreateTask" | "ViewTask" | "UpdateTask" | "DeleteTask";
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

export interface UserRequest {
  userId: UserId;
}

export interface CreateTaskRequest extends UserRequest {
  title: string;
}

export interface UpdateTaskRequest extends UserRequest {
  id: string;
  title?: string;
  completed?: boolean;
}

export interface TaskRequest extends UserRequest {
  id: string;
}

export interface PermissionRequest extends TaskRequest {
  action: "UpdateTask" | "DeleteTask";
}

export interface OidcSession {
  authenticated: boolean;
  userId?: UserId;
}

export interface RendererCedarlingOptions {
  applicationName: string;
  policyStoreDocument: PolicyStoreDocument;
}

export interface PermissionMap {
  [taskId: string]: { canUpdate: boolean; canDelete: boolean };
}

export interface ElectronApi {
  cedarling: {
    options(): Promise<RendererCedarlingOptions>;
    signedPermission(request: PermissionRequest): Promise<boolean>;
  };
  oidc: {
    login(userId: UserId): Promise<OidcSession>;
    logout(): Promise<OidcSession>;
    session(): Promise<OidcSession>;
  };
  tasks: {
    create(request: CreateTaskRequest): Promise<Task>;
    delete(request: TaskRequest): Promise<void>;
    list(request: UserRequest): Promise<Task[]>;
    update(request: UpdateTaskRequest): Promise<Task>;
  };
}
