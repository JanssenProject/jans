import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import App from '../renderer/App';

jest.mock('@janssenproject/cedarling', () => ({
  createCedarling: jest.fn(async () => ({
    ok: true,
    value: {
      authorizeUnsigned: jest.fn(async () => ({
        ok: true,
        allowed: true,
        decision: true,
        denied: false,
        value: { decision: true, diagnostics: {} },
      })),
    },
  })),
}));

const task = {
  id: 'task-1',
  title: 'Buy groceries',
  completed: false,
  owner: 'bob',
};

function installElectronBridge() {
  const invoke = jest.fn(async (channel: string) => {
    switch (channel) {
      case 'config:policy-store':
        return {};
      case 'config:test-config':
        return {
          activeScenario: 'default',
          cedarling: {},
          scenarios: [{ name: 'default' }],
        };
      case 'oidc:session':
        return { authenticated: false };
      case 'tasks:list':
        return [task];
      default:
        throw new Error(`Unexpected IPC invocation: ${channel}`);
    }
  });
  Object.defineProperty(window, 'electron', {
    configurable: true,
    value: {
      ipcRenderer: {
        invoke,
        on: jest.fn(() => jest.fn()),
      },
    },
  });
  return invoke;
}

beforeEach(() => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: jest.fn(() => ({
      matches: false,
      media: '',
      onchange: null,
      addListener: jest.fn(),
      removeListener: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      dispatchEvent: jest.fn(),
    })),
  });
  localStorage.clear();
});

describe('App', () => {
  it('renders the desktop shell and loads tasks through authorized IPC', async () => {
    const invoke = installElectronBridge();

    render(<App />);

    expect(screen.getByText('Electron desktop')).toBeInTheDocument();
    expect(await screen.findByText(task.title)).toBeInTheDocument();
    expect(invoke).toHaveBeenCalledWith('tasks:list', {
      userId: 'bob',
      signed: false,
    });
  });
});
