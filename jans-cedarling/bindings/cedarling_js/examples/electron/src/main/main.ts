import path from 'node:path';
import { app, BrowserWindow, shell } from 'electron';
import { resolveHtmlPath } from './util';
import './ipc';

let mainWindow: BrowserWindow | null = null;

function isExternalWebUrl(value: string): boolean {
  try {
    const { protocol } = new URL(value);
    return protocol === 'http:' || protocol === 'https:';
  } catch {
    return false;
  }
}

async function createWindow(): Promise<void> {
  const resourcesPath = app.isPackaged
    ? path.join(process.resourcesPath, 'assets')
    : path.join(__dirname, '../../assets');
  const preload = app.isPackaged
    ? path.join(__dirname, 'preload.js')
    : path.join(__dirname, '../../.erb/dll/preload.js');

  mainWindow = new BrowserWindow({
    show: false,
    title: 'Cedarling JS for Electron',
    width: 1180,
    height: 820,
    minWidth: 920,
    minHeight: 680,
    backgroundColor: '#f8fafc',
    icon: path.join(resourcesPath, 'icon.png'),
    autoHideMenuBar: false,
    webPreferences: {
      contextIsolation: true,
      devTools: true,
      nodeIntegration: false,
      preload,
      sandbox: true,
    },
  });

  mainWindow.once('ready-to-show', () => {
    if (!mainWindow) return;
    if (process.env.START_MINIMIZED === 'true') {
      mainWindow.minimize();
    } else {
      mainWindow.show();
    }
  });

  await mainWindow.loadURL(resolveHtmlPath('index.html'));

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (isExternalWebUrl(url)) void shell.openExternal(url);
    return { action: 'deny' };
  });
  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (url !== mainWindow?.webContents.getURL()) {
      event.preventDefault();
      if (isExternalWebUrl(url)) void shell.openExternal(url);
    }
  });

}

app.setName('Cedarling JS for Electron');

void app
  .whenReady()
  .then(async () => {
    await createWindow();
    app.on('activate', () => {
      if (mainWindow === null) void createWindow();
    });
  })
  .catch((error: unknown) => {
    console.error('[main] Failed to start Cedarling Electron:', error);
    app.quit();
  });

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
