import type { ElectronApi } from "../shared/contracts";

declare global {
  interface Window {
    electron: ElectronApi;
  }
}

export {};
