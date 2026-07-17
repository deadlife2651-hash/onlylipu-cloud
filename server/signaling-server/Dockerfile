FROM node:20-bookworm-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends adb docker.io curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY package.json ./
RUN npm install --omit=dev
COPY server.js ./
COPY admin ./admin
COPY webclient ./webclient

ENV PORT=8080
EXPOSE 8080
VOLUME /data
CMD ["node", "server.js"]
