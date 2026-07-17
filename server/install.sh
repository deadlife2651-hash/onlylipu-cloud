#!/usr/bin/env bash
# =====================================================================
#  OnlyLipu Cloud — One-command server installer (Ubuntu 22.04/24.04/26.04)
#  Usage (as root):
#    bash install.sh                      # uses 149.28.20.169.sslip.io
#    DOMAIN=cloud.yourdomain.com bash install.sh   # your own subdomain
# =====================================================================
set -euo pipefail

REPO_TARBALL="https://codeload.github.com/deadlife2651-hash/onlylipu-cloud/tar.gz/refs/heads/main"
INSTALL_DIR="/opt/onlylipu"
DOMAIN="${DOMAIN:-149.28.20.169.sslip.io}"

echo "=================================================="
echo "  ONLYLIPU CLOUD — server installer"
echo "  Domain : $DOMAIN"
echo "  Target : $INSTALL_DIR"
echo "=================================================="

if [ "$(id -u)" != "0" ]; then
  echo "ERROR: run as root (sudo -i first)"; exit 1
fi

# ---------- 1. Base packages ----------
echo "[1/7] Installing base packages…"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y curl ca-certificates gnupg ufw fail2ban tar

# ---------- 2. Docker ----------
if ! command -v docker >/dev/null 2>&1; then
  echo "[2/7] Installing Docker…"
  curl -fsSL https://get.docker.com | sh || {
    apt-get install -y docker.io docker-compose-v2
  }
else
  echo "[2/7] Docker already installed."
fi
systemctl enable --now docker

# ---------- 3. Firewall ----------
echo "[3/7] Configuring firewall (22, 80, 443 only)…"
ufw --force reset >/dev/null
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

# ---------- 4. Kernel binder modules (required by Cloud Android) ----------
echo "[4/7] Loading binder kernel modules for Cloud Android…"
modprobe binder_linux devices="binder,hwbinder,vndbinder" 2>/dev/null || \
  apt-get install -y "linux-modules-extra-$(uname -r)" 2>/dev/null || true
modprobe binder_linux devices="binder,hwbinder,vndbinder" 2>/dev/null || true
echo 'binder_linux devices="binder,hwbinder,vndbinder"' > /etc/modules-load.d/redroid.conf 2>/dev/null || true
if [ -d /dev/binderfs ] || [ -e /dev/binder ]; then
  echo "      binder OK — Cloud Android can run."
  BINDER_OK=1
else
  echo "      WARNING: binder module not available on this kernel."
  echo "      Cloud Android (redroid) needs it. The installer continues —"
  echo "      see docs/01, section 'Binder সমস্যা'."
  BINDER_OK=0
fi

# ---------- 5. Download OnlyLipu server package ----------
echo "[5/7] Downloading OnlyLipu Cloud server package…"
mkdir -p "$INSTALL_DIR"
TMP=$(mktemp -d)
curl -fsSL "$REPO_TARBALL" -o "$TMP/repo.tar.gz"
tar -xzf "$TMP/repo.tar.gz" -C "$TMP"
cp -r "$TMP"/onlylipu-cloud-*/server/* "$INSTALL_DIR"/
rm -rf "$TMP"
cd "$INSTALL_DIR"

# ---------- 6. Secrets ----------
echo "[6/7] Generating secure credentials…"
if [ ! -f .env ]; then
  ADMIN_PASS=$(tr -dc 'A-Za-z0-9!@#%^_+=' </dev/urandom | head -c 20)
  DESK_PASS=$(tr -dc 'A-Za-z0-9' </dev/urandom | head -c 16)
  JWT=$(head -c 64 /dev/urandom | od -An -tx1 | tr -d ' \n')
  cat > .env <<ENVEOF
DOMAIN=$DOMAIN
ADMIN_USER=admin
ADMIN_PASSWORD=$ADMIN_PASS
DESKTOP_PASSWORD=$DESK_PASS
JWT_SECRET=$JWT
SERVER_LOCATION=Tokyo, JP
ENVEOF
  chmod 600 .env
else
  sed -i "s|^DOMAIN=.*|DOMAIN=$DOMAIN|" .env
fi

# ---------- 7. Build & start ----------
echo "[7/7] Building and starting all services…"
docker compose pull --quiet android desktop caddy || true
docker compose up -d --build

sleep 8
echo
echo "Waiting for HTTPS certificate (first run can take ~60s)…"
for i in $(seq 1 12); do
  if curl -sk "https://$DOMAIN/api/status" -o /dev/null; then break; fi
  sleep 5
done

# ---------- Credentials file ----------
cat > /root/onlylipu-credentials.txt <<CREDEOF
ONLYLIPU CLOUD — credentials (keep this file secret, chmod 600)
==============================================================
Admin panel : https://$DOMAIN/admin
Username    : admin
Password    : $(grep '^ADMIN_PASSWORD=' .env | cut -d= -f2-)

Cloud Computer (browser): https://$DOMAIN/desktop/vnc.html?autoconnect=true&resize=scale&path=desktop/websockify
Desktop VNC password    : $(grep '^DESKTOP_PASSWORD=' .env | cut -d= -f2-)

Cloud Android (redroid) : adb connect <server-ip>:5555 (SSH tunnel recommended)
Binder kernel module    : $([ "${BINDER_OK:-0}" = "1" ] && echo OK || echo MISSING — see docs)

App server URL (put in Android Studio local.properties):
ONLYLIPU_SERVER_URL=https://$DOMAIN
==============================================================
Generated: $(date -u)
CREDEOF
chmod 600 /root/onlylipu-credentials.txt

echo
echo "=================================================="
echo "  ✅ INSTALL COMPLETE"
echo "  Admin panel : https://$DOMAIN/admin"
echo "  Credentials : /root/onlylipu-credentials.txt"
echo "  Binder      : $([ "${BINDER_OK:-0}" = "1" ] && echo OK || echo MISSING)"
echo "=================================================="
docker compose ps
