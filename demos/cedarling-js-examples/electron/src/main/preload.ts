import { contextBridge, ipcRenderer } from 'electron';

export type Channels =
  | 'tasks:list'
  | 'tasks:create'
  | 'tasks:update'
  | 'tasks:delete'
  | 'authorize-signed'
  | 'oidc:session'
  | 'oidc:login'
  | 'oidc:logout'
  | 'config:policy-store'
  | 'config:test-config';

const electronHandler = {
  ipcRenderer: {
    invoke<T>(channel: Channels, ...args: unknown[]): Promise<T> {
      return ipcRenderer.invoke(channel, ...args) as Promise<T>;
    },
  },
};

contextBridge.exposeInMainWorld('electron', electronHandler);

export type ElectronHandler = typeof electronHandler;
