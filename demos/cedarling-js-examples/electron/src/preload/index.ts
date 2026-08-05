import { contextBridge, ipcRenderer } from "electron";

import type { ElectronApi } from "../shared/contracts";

// Expose explicit operations rather than ipcRenderer itself; main validates
// every payload and remains the authorization boundary.
const electronApi: ElectronApi = {
  cedarling: {
    options: () => ipcRenderer.invoke("cedarling:options"),
    signedPermission: (request) => ipcRenderer.invoke("cedarling:signed-permission", request),
  },
  oidc: {
    login: (userId) => ipcRenderer.invoke("oidc:login", { userId }),
    logout: () => ipcRenderer.invoke("oidc:logout"),
    session: () => ipcRenderer.invoke("oidc:session"),
  },
  tasks: {
    create: (request) => ipcRenderer.invoke("tasks:create", request),
    delete: async (request) => {
      await ipcRenderer.invoke("tasks:delete", request);
    },
    list: (request) => ipcRenderer.invoke("tasks:list", request),
    update: (request) => ipcRenderer.invoke("tasks:update", request),
  },
};

contextBridge.exposeInMainWorld("electron", electronApi);
