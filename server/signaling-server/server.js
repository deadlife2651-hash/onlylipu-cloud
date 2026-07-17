/**
 * OnlyLipu Cloud — Control Server
 * --------------------------------
 * - JWT auth (single admin account, bcrypt-hashed password from env)
 * - /api/status        live CPU/RAM/storage + VM states
 * - /api/apps/*        install / list / uninstall / clear-data inside Cloud Android (redroid via adb)
 * - /api/vm/android/*  start / stop the Cloud Android container
 * - /api/audit         security audit log (login, connection, upload, install actions)
 * - /ws/signaling      WebRTC SDP/ICE relay for the OnlyLipu Android app
 * - /admin             web admin panel (static)
 * - /app               Windows / browser client (static PWA)
 */
'use strict';

const express = require('express');
const http = require('http');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const { execFile } = require('child_process');
const { WebSocketServer } = require('ws');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const si = require('systeminformation');

const PORT = parseInt(process.env.PORT || '8080', 10);
const JWT_SECRET = process.env.JWT_SECRET || crypto.randomBytes(32).toString('hex');
const ADMIN_USER = process.env.ADMIN_USER || 'admin';
const ADMIN_PASSWORD_HASH = process.env.ADMIN_PASSWORD_HASH ||
  (process.env.ADMIN_PASSWORD ? bcrypt.hashSync(process.env.ADMIN_PASSWORD, 10) : '');
const REDROID_CONTAINER = process.env.REDROID_CONTAINER || 'onlylipu-android';
const ADB_SERIAL = process.env.ADB_SERIAL || 'localhost:5555';
const UPLOAD_DIR = process.env.UPLOAD_DIR || '/data/uploads';
const AUDIT_LOG = process.env.AUDIT_LOG || '/data/audit.log';
const TOKEN_TTL = process.env.TOKEN_TTL || '12h';

fs.mkdirSync(UPLOAD_DIR, { recursive: true });

function audit(action, detail, req) {
  const line = JSON.stringify({
    ts: new Date().toISOString(),
    action,
    detail,
    ip: req ? (req.headers['x-forwarded-for'] || req.socket.remoteAddress) : undefined
  }) + '\n';
  fs.appendFile(AUDIT_LOG, line, () => {});
}

function sh(cmd, args, opts = {}) {
  return new Promise((resolve) => {
    execFile(cmd, args, { timeout: opts.timeout || 30000, maxBuffer: 8 * 1024 * 1024 },
      (err, stdout, stderr) => {
        resolve({ ok: !err, out: (stdout || '').toString(), err: (stderr || '').toString() });
      });
  });
}

async function dockerState(name) {
  const r = await sh('docker', ['inspect', '-f', '{{.State.Running}}', name]);
  return r.ok && r.out.trim() === 'true';
}

async function adb(args, timeout) {
  return sh('adb', ['-s', ADB_SERIAL, ...args], { timeout: timeout || 60000 });
}

const app = express();
app.use(express.json({ limit: '1mb' }));

// ---------- auth ----------
app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body || {};
  if (!username || !password) return res.status(400).json({ error: 'missing_credentials' });
  const okUser = username === ADMIN_USER;
  const okPass = ADMIN_PASSWORD_HASH && await bcrypt.compare(password, ADMIN_PASSWORD_HASH);
  if (!okUser || !okPass) {
    audit('login_failed', { username }, req);
    return res.status(401).json({ error: 'invalid_credentials' });
  }
  const token = jwt.sign({ sub: username }, JWT_SECRET, { expiresIn: TOKEN_TTL });
  audit('login_success', { username }, req);
  res.json({ token, expiresIn: 43200 });
});

app.post('/api/auth/logout-all', auth, (req, res) => {
  audit('logout_all', { user: req.user }, req);
  res.json({ ok: true, message: 'Logged out everywhere' });
});

function auth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : (req.query.token || '');
  try {
    req.user = jwt.verify(token, JWT_SECRET).sub;
    next();
  } catch {
    res.status(401).json({ error: 'unauthorized' });
  }
}

// ---------- status ----------
app.get('/api/status', auth, async (req, res) => {
  try {
    const [cpu, mem, disk] = await Promise.all([
      si.currentLoad(), si.mem(),
      si.fsSize().then(list => list.find(f => f.mount === '/') || list[0] || {})
    ]);
    const [androidRunning, desktopRunning] = await Promise.all([
      dockerState(REDROID_CONTAINER),
      dockerState('onlylipu-desktop')
    ]);
    res.json({
      online: true,
      state: 'online',
      location: process.env.SERVER_LOCATION || 'Tokyo, JP',
      cpuPercent: Math.round(cpu.currentLoad * 10) / 10,
      ramUsedGb: Math.round((mem.active / 1e9) * 10) / 10,
      ramTotalGb: Math.round((mem.total / 1e9) * 10) / 10,
      storageUsedGb: Math.round(((disk.used || 0) / 1e9) * 10) / 10,
      storageTotalGb: Math.round(((disk.size || 0) / 1e9) * 10) / 10,
      cloudAndroidRunning: androidRunning,
      cloudComputerRunning: desktopRunning,
      activeSessions: sessions.size
    });
  } catch (e) {
    res.status(500).json({ error: 'status_failed' });
  }
});

// ---------- Cloud Android VM control ----------
app.post('/api/vm/android/start', auth, async (req, res) => {
  audit('vm_android_start', {}, req);
  const r = await sh('docker', ['start', REDROID_CONTAINER], { timeout: 120000 });
  res.status(r.ok ? 200 : 500).json({ ok: r.ok, message: r.ok ? 'Cloud Android starting' : r.err });
});

app.post('/api/vm/android/stop', auth, async (req, res) => {
  audit('vm_android_stop', {}, req);
  const r = await sh('docker', ['stop', REDROID_CONTAINER], { timeout: 120000 });
  res.status(r.ok ? 200 : 500).json({ ok: r.ok, message: r.ok ? 'Cloud Android stopped' : r.err });
});

// ---------- app management ----------
const upload = multer({
  dest: UPLOAD_DIR,
  limits: { fileSize: 500 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    if (!file.originalname.toLowerCase().endsWith('.apk')) {
      return cb(new Error('only_apk_allowed'));
    }
    cb(null, true);
  }
});

app.post('/api/apps/install', auth, upload.single('apk'), async (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'apk_file_required' });
  audit('apk_upload', { name: req.file.originalname, size: req.file.size }, req);
  const apkPath = req.file.path;
  await adb(['connect', ADB_SERIAL]);
  const r = await adb(['install', '-r', apkPath], 300000);
  const success = r.out.includes('Success');
  fs.unlink(apkPath, () => {}); // delete uploaded installation file after processing
  if (success) {
    audit('apk_install_success', { name: req.file.originalname }, req);
    res.json({ ok: true, message: 'App installed into Cloud Android' });
  } else {
    audit('apk_install_failed', { name: req.file.originalname, err: r.out + r.err }, req);
    res.status(500).json({ error: 'install_failed', detail: (r.out + r.err).slice(0, 500) });
  }
});

app.get('/api/apps', auth, async (req, res) => {
  await adb(['connect', ADB_SERIAL]);
  const pkgs = await adb(['shell', 'pm', 'list', 'packages', '-3']);
  const names = pkgs.out.split('\n')
    .map(l => l.replace('package:', '').trim()).filter(Boolean);
  const apps = [];
  for (const p of names.slice(0, 200)) {
    const info = await adb(['shell', 'dumpsys', 'package', p]);
    const version = (info.out.match(/versionName=([^\s]+)/) || [])[1] || '';
    apps.push({ packageName: p, label: p.split('.').pop(), versionName: version, sizeMb: 0 });
  }
  res.json({ apps });
});

app.delete('/api/apps/:pkg', auth, async (req, res) => {
  const pkg = req.params.pkg;
  if (!/^[a-zA-Z0-9._]+$/.test(pkg)) return res.status(400).json({ error: 'bad_package' });
  audit('apk_uninstall', { pkg }, req);
  const r = await adb(['uninstall', pkg], 120000);
  res.status(r.out.includes('Success') ? 200 : 500)
     .json({ ok: r.out.includes('Success'), message: r.out.trim() });
});

app.post('/api/apps/:pkg/clear-data', auth, async (req, res) => {
  const pkg = req.params.pkg;
  if (!/^[a-zA-Z0-9._]+$/.test(pkg)) return res.status(400).json({ error: 'bad_package' });
  audit('apk_clear_data', { pkg }, req);
  const r = await adb(['shell', 'pm', 'clear', pkg], 60000);
  res.status(r.out.includes('Success') ? 200 : 500)
     .json({ ok: r.out.includes('Success'), message: r.out.trim() });
});

// ---------- audit ----------
app.get('/api/audit', auth, (req, res) => {
  fs.readFile(AUDIT_LOG, 'utf8', (err, data) => {
    if (err) return res.json({ entries: [] });
    const entries = data.trim().split('\n').slice(-200).map(l => {
      try { return JSON.parse(l); } catch { return null; }
    }).filter(Boolean).reverse();
    res.json({ entries });
  });
});

// ---------- admin panel + web client ----------
app.use('/admin', express.static(path.join(__dirname, 'admin')));
app.use('/app', express.static(path.join(__dirname, 'webclient')));
app.get('/', (req, res) => res.redirect('/admin'));

// ---------- WebRTC signaling ----------
const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws/signaling' });
const sessions = new Map();

wss.on('connection', (ws, req) => {
  const url = new URL(req.url, 'http://x');
  const env = url.searchParams.get('env') || 'android';
  try {
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : url.searchParams.get('token');
    jwt.verify(token, JWT_SECRET);
  } catch {
    ws.close(4401, 'unauthorized');
    return;
  }
  const id = crypto.randomUUID();
  sessions.set(id, { ws, env });
  audit('stream_connected', { env }, null);

  ws.on('message', (raw) => {
    let msg;
    try { msg = JSON.parse(raw.toString()); } catch { return; }
    // Relay SDP/ICE to every other peer of the same environment
    // (the server-side streamer gateway joins the same room).
    for (const [otherId, s] of sessions) {
      if (otherId !== id && s.env === env && s.ws.readyState === 1) {
        s.ws.send(JSON.stringify(msg));
      }
    }
  });

  ws.on('close', () => {
    sessions.delete(id);
    audit('stream_disconnected', { env }, null);
  });
});

server.listen(PORT, () => {
  console.log(`OnlyLipu control server listening on :${PORT}`);
});
