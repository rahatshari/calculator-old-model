const CACHE_NAME = 'app-cache-v2';
const urlsToCache = [
    './',
    './index.html',
    './manifest.json',
    './icon.png'
];

// ১. Install Event: বেসিক ফাইল ক্যাশ করা এবং skipWaiting ব্যবহার
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => {
            return cache.addAll(urlsToCache);
        })
    );
    self.skipWaiting(); // নতুন সার্ভিস ওয়ার্কার ইনস্টল হওয়ামাত্রই এক্টিভ করবে
});

// ২. Activate Event: পুরানো ক্যাশ ডিলিট করা এবং clients.claim ব্যবহার
self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames.map(cache => {
                    if (cache !== CACHE_NAME) {
                        return caches.delete(cache); // পুরানো ভার্সনের ক্যাশ মুছে ফেলবে
                    }
                })
            );
        })
    );
    self.clients.claim(); // পেজ রিলোড ছাড়াই নতুন সার্ভিস ওয়ার্কারের নিয়ন্ত্রণ নেবে
});

// ৩. Fetch Event: Cache First স্ট্র্যাটেজি এবং ফলব্যাক
self.addEventListener('fetch', event => {
    event.respondWith(
        caches.match(event.request).then(cachedResponse => {
            // ক্যাশে ফাইল পাওয়া গেলে সেটি রিটার্ন করবে (Cache First)
            if (cachedResponse) {
                return cachedResponse;
            }

            // ক্যাশে না থাকলে নেটওয়ার্ক থেকে ফাইল ফেচ করার চেষ্টা করবে
            return fetch(event.request).catch(() => {
                // ইন্টারনেট না থাকলে এবং রিকোয়েস্টটি কোনো পেজ নেভিগেশনের হলে ডিফল্ট index.html শো করবে
                if (event.request.mode === 'navigate' || event.request.headers.get('accept').includes('text/html')) {
                    return caches.match('./index.html');
                }
            });
        })
    );
});
