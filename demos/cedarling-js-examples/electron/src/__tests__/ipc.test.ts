type IpcHandler = (event: unknown, ...args: unknown[]) => unknown | Promise<unknown>;

const handlers = new Map<string, IpcHandler>();
const mockAuthorizeAction = jest.fn();

jest.mock("electron", () => ({
  ipcMain: {
    handle: jest.fn((channel: string, handler: IpcHandler) => handlers.set(channel, handler)),
    removeHandler: jest.fn((channel: string) => handlers.delete(channel)),
  },
  shell: { openExternal: jest.fn() },
}));

jest.mock("../main/cedarling/authorize", () => ({
  authorizeAction: (...args: unknown[]) => mockAuthorizeAction(...args),
}));

jest.mock("../main/cedarling/config", () => ({
  loadCedarlingOptions: jest.fn(async () => ({ applicationName: "TaskApp", policyStoreDocument: {} })),
  oidcAllowsInsecureRequests: jest.fn(() => true),
  oidcIssuer: jest.fn(() => "http://localhost:9090"),
}));

beforeAll(() => {
  jest.requireActual("../main/ipc");
});

beforeEach(() => {
  mockAuthorizeAction.mockReset();
  mockAuthorizeAction.mockResolvedValue("allowed");
});

describe("main-process IPC boundary", () => {
  it("authorizes listed tasks using main-owned resource attributes", async () => {
    const result = await handlers.get("tasks:list")?.({}, { userId: "bob" });
    expect(result).toHaveLength(2);
    expect(mockAuthorizeAction).toHaveBeenNthCalledWith(
      1,
      "ViewTask",
      "bob",
      {
        type: "TaskApp::Task",
        id: "task-1",
        attributes: { owner: "bob", title: "Buy groceries", completed: false },
      },
      undefined,
    );
  });

  it("rejects unknown and unnecessary fields at runtime", async () => {
    await expect(
      handlers.get("tasks:create")?.({}, { userId: "bob", title: "Task", owner: "alice" }),
    ).rejects.toThrow(/unsupported field/);
    await expect(handlers.get("tasks:list")?.({}, { userId: "mallory" })).rejects.toThrow(/Unknown/);
  });

  it("resolves missing tasks before authorization", async () => {
    await expect(
      handlers.get("tasks:delete")?.({}, { userId: "bob", id: "missing" }),
    ).rejects.toThrow("Task not found");
    expect(mockAuthorizeAction).not.toHaveBeenCalled();
  });

  it("does not expose signed permission checks without a main-owned session", async () => {
    await expect(
      handlers.get("cedarling:signed-permission")?.(
        {},
        { userId: "bob", id: "task-1", action: "UpdateTask" },
      ),
    ).resolves.toBe(false);
  });
});
