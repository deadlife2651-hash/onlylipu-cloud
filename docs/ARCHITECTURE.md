# OnlyLipu Cloud — Architecture & Honest Status

## Components
```
┌──────────────────────┐         HTTPS / WSS          ┌─────────────────────────────┐
│  Android app (Kotlin) │ ◄──────────────────────────► │  Vultr server (Tokyo)       │
│  Windows client (PWA/ │                              │  ┌─────────┐  ┌───────────┐ │
│  Electron)            │                              │  │ Caddy   │→│ control   │ │
└──────────────────────┘                              │  │ (TLS)   │  │ (Node API │ │
                                                      │  └─────────┘  │  + admin  │ │
                                                      │               │  + signal)│ │
                                                      │               └─────┬─────┘ │
                                                      │        docker.sock / adb    │
                                                      │          ┌─────────┴──────┐ │
                                                      │          ▼                ▼ │
                                                      │   ┌────────────┐  ┌────────┐│
                                                      │   │ redroid    │  │ LXDE   ││
                                                      │   │ Android 15 │  │ desktop││
                                                      │   │ (8GB/4CPU) │  │ +noVNC ││
                                                      │   └────────────┘  └────────┘│
                                                      └─────────────────────────────┘
```

## API surface (control server)
| Method | Path | Purpose |
|---|---|---|
| POST | /api/auth/login | username+password → JWT (12h) |
| POST | /api/auth/logout-all | end all sessions (audited) |
| GET  | /api/status | CPU/RAM/storage, VM states, sessions |
| GET  | /api/apps | user apps inside Cloud Android |
| POST | /api/apps/install | multipart APK upload → adb install |
| DELETE | /api/apps/{pkg} | uninstall |
| POST | /api/apps/{pkg}/clear-data | pm clear |
| POST | /api/vm/android/start · /stop | container control |
| GET  | /api/audit | last 200 audit entries |
| WS   | /ws/signaling | WebRTC SDP/ICE relay per environment |

## What works today (v1.0)
- Full server stack: TLS, auth, admin panel, audit log, APK install into
  Cloud Android, VM start/stop, live status.
- Cloud Computer streaming in browser/Windows client via noVNC (works now).
- Cloud Android accessible via adb/scrcpy (works now, great on laptop).
- Android app: complete UI, login, dashboard, app manager, settings,
  WebRTC client + signaling code.

## Known gaps / roadmap (honest)
1. **WebRTC media gateway (server side)** — the app and signaling channel are
   ready; a server-side bridge (scrcpy-server / Sunshine → WebRTC, e.g. via
   GStreamer webrtcbin or Pion) is the remaining piece for native in-app
   full-screen streaming. Until then, Cloud Android on laptop = scrcpy,
   Cloud Computer = noVNC. This is Phase 2.
2. **Android 16** — redroid's newest image is Android 15. The "Android 16"
   label ships when redroid publishes it (drop-in image swap in
   docker-compose.yml).
3. **TURN server** — for restrictive mobile networks, add coturn.
4. Cloud Android in-browser streaming (ws-scrcpy) can be added as an
   optional compose profile.
