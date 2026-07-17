const { app, BrowserWindow, shell, Menu } = require('electron');
const path = require('path');
const fs = require('fs');

function loadConfig() {
  try {
    const cfgPath = path.join(
      app.isPackaged ? path.dirname(app.getPath('exe')) : __dirname,
      'config.json'
    );
    return JSON.parse(fs.readFileSync(cfgPath, 'utf8'));
  } catch {
    return { serverUrl: 'https://149.28.20.169.sslip.io' };
  }
}

let win;

function createWindow() {
  const { serverUrl } = loadConfig();
  win = new BrowserWindow({
    width: 1180,
    height: 780,
    minWidth: 900,
    minHeight: 600,
    backgroundColor: '#0B0E13',
    title: 'OnlyLipu Cloud',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  Menu.setApplicationMenu(null);
  win.loadURL(serverUrl.replace(/\/$/, '') + '/app/');

  // Open external links (e.g. scrcpy download) in the system browser.
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (!url.startsWith(serverUrl)) {
      shell.openExternal(url);
      return { action: 'deny' };
    }
    return { action: 'allow' };
  });
}

app.whenReady().then(createWindow);
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });
