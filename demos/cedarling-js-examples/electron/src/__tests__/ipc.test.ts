type IpcHandler = (
  event: unknown,
  ...args: unknown[]
) => unknown | Promise<unknown>;

const mockHandlers = new Map<string, IpcHandler>();
const mockAuthorizeAction = jest.fn();

jest.mock('electron', () => ({
  ipcMain: {
    handle: jest.fn((channel: string, handler: IpcHandler) => {
      mockHandlers.set(channel, handler);
    }),
  },
  shell: { openExternal: jest.fn() },
}));

jest.mock('../main/cedarling/authorize', () => ({
  authorizeAction: (...args: unknown[]) => mockAuthorizeAction(...args),
}));

jest.mock('../main/cedarling/init', () => ({
  loadPolicyStore: jest.fn(() => ({})),
  loadTestConfig: jest.fn(() => ({})),
}));

beforeAll(() => {
  jest.requireActual('../main/ipc');
});

beforeEach(() => {
  mockAuthorizeAction.mockReset();
  mockAuthorizeAction.mockResolvedValue({ allowed: true });
});

describe('main-process task authorization', () => {
  it('authorizes every listed task with schema-valid resource attributes', async () => {
    const listTasks = mockHandlers.get('tasks:list');
    expect(listTasks).toBeDefined();

    const result = await listTasks?.({}, { userId: 'bob', signed: false });

    expect(result).toEqual([
      {
        id: 'task-1',
        title: 'Buy groceries',
        completed: false,
        owner: 'bob',
      },
      {
        id: 'task-2',
        title: 'Schedule meeting with CEO',
        completed: false,
        owner: 'alice',
      },
    ]);
    expect(mockAuthorizeAction).toHaveBeenCalledTimes(2);
    expect(mockAuthorizeAction).toHaveBeenNthCalledWith(
      1,
      'ViewTask',
      'bob',
      {
        type: 'TaskApp::Task',
        id: 'task-1',
        attributes: {
          owner: 'bob',
          title: 'Buy groceries',
          completed: false,
        },
      },
      undefined,
    );
    expect(mockAuthorizeAction).toHaveBeenNthCalledWith(
      2,
      'ViewTask',
      'bob',
      {
        type: 'TaskApp::Task',
        id: 'task-2',
        attributes: {
          owner: 'alice',
          title: 'Schedule meeting with CEO',
          completed: false,
        },
      },
      undefined,
    );
  });
});
