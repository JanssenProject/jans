import path from "node:path";
import { app, BrowserWindow } from "electron";

import { shutDownCedarling } from "./cedarling/init";
import "./ipc";
import { resolveHtmlPath } from "./util";

let mainWindow: BrowserWindow | null = null;
let shutdownStarted = false;

async function createWindow(): Promise<void> {
  const resourcesPath = app.isPackaged
    ? path.join(process.resourcesPath, "assets")
    : path.join(__dirname, "../../assets");
  const preload = app.isPackaged
    ? path.join(__dirname, "preload.js")
    : path.join(__dirname, "../../.erb/dll/preload.bundle.dev.js");
  mainWindow = new BrowserWindow({
    show: false,
    title: "Cedarling JS for Electron",
    width: 1180,
    height: 820,
    minWidth: 920,
    minHeight: 680,
    backgroundColor: "#fafbfe",
    icon: path.join(resourcesPath, "icon.png"),
    autoHideMenuBar: false,
    webPreferences: {
      contextIsolation: true,
      devTools: !app.isPackaged,
      nodeIntegration: false,
      preload,
      sandbox: true,
    },
  });
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: "deny" }));
  mainWindow.webContents.on("will-navigate", (event, url) => {
    if (url !== mainWindow?.webContents.getURL()) event.preventDefault();
  });
  mainWindow.once("ready-to-show", () => {
    if (process.env.START_MINIMIZED === "true") mainWindow?.minimize();
    else mainWindow?.show();
  });
  mainWindow.on("closed", () => {
    mainWindow = null;
  });
  await mainWindow.loadURL(resolveHtmlPath("index.html"));
}

app.setName("Cedarling JS for Electron");
void app
  .whenReady()
  .then(async () => {
    await createWindow();
    app.on("activate", () => {
      if (!mainWindow) void createWindow();
    });
  })
  .catch((error) => {
    console.error("[main] Failed to start TaskApp", error);
    app.quit();
  });

// Electron may quit while authorization promises are active; delay the final
// quit until the main-process Cedarling engine has drained them.
app.on("before-quit", (event) => {
  if (shutdownStarted) return;
  shutdownStarted = true;
  event.preventDefault();
  void shutDownCedarling()
    .catch((error) => console.error("[main] Cedarling shutdown failed", error))
    .finally(() => app.quit());
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
