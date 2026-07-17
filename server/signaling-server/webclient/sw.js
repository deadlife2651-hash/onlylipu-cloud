const CACHE = 'onlylipu-v1';
self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(['/app/'])));
  self.skipWaiting();
});
self.addEventListener('fetch', e => {
  if (e.request.url.includes('/api/')) return;   // never cache API
  e.respondWith(
    caches.match(e.request).then(r => r || fetch(e.request))
  );
});
