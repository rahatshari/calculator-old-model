const CACHE_NAME = 'jami-wood-offline-v3';
const PRECACHE_URLS = [
    '/',
    '/index.html',
    '/manifest.json',
    '/icon.png',
    'https://fonts.googleapis.com/css2?family=Noto+Sans+Bengali:wght@400;500;600;700&display=swap'
];

// 1. Install Event: Cache all critical assets
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME).then(async cache => {
            // Cache local URLs
            for (const url of PRECACHE_URLS) {
                try {
                    await cache.add(url);
                } catch (err) {
                    console.warn('[SW] Could not precache url:', url, err);
                }
            }
        })
    );
    self.skipWaiting();
});

// 2. Activate Event: Clean up previous caches and take control
self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys => {
            return Promise.all(
                keys.map(key => {
                    if (key !== CACHE_NAME) {
                        return caches.delete(key);
                    }
                })
            );
        }).then(() => self.clients.claim())
    );
});

// 3. Fetch Event: Offline-first with cache fallback
self.addEventListener('fetch', event => {
    const request = event.request;

    // Ignore non-GET requests
    if (request.method !== 'GET') return;

    // Handle HTML navigations: Network First, fallback to Cache
    if (request.mode === 'navigate' || (request.headers.get('accept') && request.headers.get('accept').includes('text/html'))) {
        event.respondWith(
            fetch(request)
                .then(networkResponse => {
                    if (networkResponse && networkResponse.status === 200) {
                        const responseClone = networkResponse.clone();
                        caches.open(CACHE_NAME).then(cache => cache.put(request, responseClone));
                    }
                    return networkResponse;
                })
                .catch(() => {
                    return caches.match('/index.html')
                        .then(cachedHtml => cachedHtml || caches.match('/'));
                })
        );
        return;
    }

    // Handle static assets & external fonts: Cache First, fallback to Network
    event.respondWith(
        caches.match(request).then(cachedResponse => {
            if (cachedResponse) {
                // Fetch in background to update cache if online
                fetch(request).then(networkResponse => {
                    if (networkResponse && networkResponse.status === 200) {
                        caches.open(CACHE_NAME).then(cache => cache.put(request, networkResponse));
                    }
                }).catch(() => {/* Ignore background fetch failures when offline */});
                return cachedResponse;
            }

            return fetch(request).then(networkResponse => {
                if (networkResponse && networkResponse.status === 200) {
                    const responseClone = networkResponse.clone();
                    caches.open(CACHE_NAME).then(cache => cache.put(request, responseClone));
                }
                return networkResponse;
            }).catch(() => {
                // If offline and requesting an image, fallback to icon if available
                if (request.destination === 'image') {
                    return caches.match('/icon.png');
                }
            });
        })
    );
});
