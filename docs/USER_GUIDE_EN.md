# OnlyLipu Cloud — User Guide (English)

## First run
1. Open the app → splash → sign in with the admin credentials
   (found in `/root/onlylipu-credentials.txt` on the server).
2. Optionally enable **Biometric unlock** in Settings.

## Dashboard
- Server status (Online/Offline), latency, location, CPU/RAM/Storage bars.
- Two cards: **Cloud Computer** ("Your dedicated desktop") and
  **Cloud Android** ("Your private Android phone").
- One-tap **Reconnect** returns to your last environment.

## Cloud Android
- Tap / long-press / two-finger scroll / pinch — like a physical phone.
- Floating bar auto-hides: Back, input mode, remote keyboard
  (with Back/Home/Recent), quality menu, audio mute, stats overlay.
- **Install apps:** Dashboard → robot icon → *Install App (APK)* → pick an APK
  from your phone → watch upload % → it appears in the Cloud Android app drawer.
  Uninstall and Clear data live on the same page.

## Cloud Computer
- Single tap = left click, long press = right click, two-finger = scroll.
- Switch mouse/touch mode from the floating bar.
- On your laptop: open `https://<your-domain>/app` and click the card —
  full-screen desktop in the browser (or install it as a Windows app).

## Streaming quality
HD icon in the floating bar: Auto / Data Saver / 720p 60 / 1080p 60 / Highest.
Use Auto or Data Saver on weak networks.

## Security
- All traffic is HTTPS; sessions use short-lived tokens stored in
  Android Keystore-encrypted storage.
- Every login, connection, upload and install is written to the server
  audit log (visible in the admin panel).
- *Log out from all devices* is in Settings.

## Troubleshooting
| Symptom | Fix |
|---|---|
| "Server offline" | Start Cloud Android from the admin panel, or restart the server in Vultr |
| Black stream | Lower quality to Data Saver; switch network |
| APK install fails | APK must be < 500 MB; check the VM is running in the admin panel |
| Login rejected | Re-check credentials in /root/onlylipu-credentials.txt |
