import path from "node:path";
import { app, BrowserWindow, protocol } from "electron";

import { oidcIssuer } from "./cedarling/config";
import { shutDownCedarling } from "./cedarling/init";
import "./ipc";
import {
  installRendererContentSecurityPolicy,
  rendererAssetResponse,
} from "./security";

const rendererScheme = "app";
const rendererOrigin = `${rendererScheme}://renderer`;

protocol.registerSchemesAsPrivileged([
  {
    scheme: rendererScheme,
    privileges: {
      corsEnabled: true,
      secure: true,
      standard: true,
      supportFetchAPI: true,
    },
  },
]);

let mainWindow: BrowserWindow | null = null;
let shutdownStarted = false;

async function createWindow(): Promise<void> {
  const isDevelopment = Boolean(process.env.ELECTRON_RENDERER_URL);
  const preload = path.join(__dirname, "../preload/index.js");
  mainWindow = new BrowserWindow({
    show: false,
    title: "Cedarling JS for Electron",
    width: 1180,
    height: 820,
    minWidth: 920,
    minHeight: 680,
    backgroundColor: "#087846",
    icon: path.join(__dirname, "../../assets/icon.png"),
    autoHideMenuBar: false,
    webPreferences: {
      contextIsolation: true,
      devTools: isDevelopment,
      nodeIntegration: false,
      preload,
      sandbox: true,
    },
  });
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: "deny" }));
  mainWindow.webContents.session.setPermissionCheckHandler(() => false);
  mainWindow.webContents.session.setPermissionRequestHandler(
    (_webContents, _permission, callback) => callback(false),
  );
  mainWindow.webContents.on("will-navigate", (event, url) => {
    if (url !== mainWindow?.webContents.getURL()) event.preventDefault();
  });
  const rendererUrl =
    process.env.ELECTRON_RENDERER_URL ?? `${rendererOrigin}/index.html`;
  if (isDevelopment) {
    // The development server supplies the document, so apply the same policy
    // as the built app through Electron's response-header hook.
    installRendererContentSecurityPolicy(
      mainWindow.webContents,
      rendererUrl,
      oidcIssuer(),
    );
  }
  mainWindow.once("ready-to-show", () => {
    if (process.env.START_MINIMIZED === "true") mainWindow?.minimize();
    else mainWindow?.show();
  });
  mainWindow.on("closed", () => {
    mainWindow = null;
  });
  await mainWindow.loadURL(rendererUrl);
}

app.setName("Cedarling JS for Electron");
void app
  .whenReady()
  .then(async () => {
    const rendererRoot = path.join(__dirname, "../renderer");
    protocol.handle(rendererScheme, (request) =>
      rendererAssetResponse(rendererRoot, request.url, oidcIssuer()),
    );
    await createWindow();
    app.on("activate", () => {
      if (!mainWindow) void createWindow();
    });
  })
  .catch((error) => {
    console.error("[main] Failed to start TaskApp", error);
    app.quit();
  });

function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((_, reject) => {
      const timer = setTimeout(
        () => reject(new Error(`Operation timed out after ${ms}ms`)),
        ms,
      );
      timer.unref?.();
    }),
  ]);
}

// Electron may quit while authorization promises are active; delay the final
// quit until the main-process Cedarling engine has drained them.
app.on("before-quit", (event) => {
  if (shutdownStarted) return;
  shutdownStarted = true;
  event.preventDefault();
  void withTimeout(shutDownCedarling(), 5000)
    .catch((error) => console.error("[main] Cedarling shutdown failed", error))
    .finally(() => app.quit());
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
