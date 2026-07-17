# OnlyLipu Cloud — Windows 11 অ্যাপ বিল্ড (বাংলা)

## সহজ উপায় (কোনো বিল্ড লাগে না)
সার্ভার চালু হলে Edge বা Chrome খুলে যান:
`https://149.28.20.169.sslip.io/app`
ডান-উপরে মেনু → **Apps → Install OnlyLipu Cloud** — হয়ে গেলো,
স্টার্ট মেনুতে আসল অ্যাপের মতো থাকবে।

## আসল .exe ইনস্টলার বানাতে চাইলে (ঐচ্ছিক)
ল্যাপটপে একবারের সেটআপ:

1. https://nodejs.org থেকে Node.js LTS ইনস্টল করুন (Next-Next-Finish)।
2. এই `windows-client` ফোল্ডারটা ল্যাপটপে কপি করুন।
3. ফোল্ডারে টার্মিনাল খুলুন (ফোল্ডারে Shift+Right-click → "Open in Terminal")।
4. চালান:

```powershell
npm install
npm run dist
```

5. `dist/` ফোল্ডারে **OnlyLipu Cloud Setup x.x.x.exe** তৈরি হবে — ডাবল-ক্লিক করে ইনস্টল।

## নিজের সাবডোমেইন বসালে
`config.json` ফাইলে `"serverUrl"` বদলে দিন, তারপর আবার `npm run dist`।

## icon.ico
যেকোনো 256x256 PNG-কে https://icoconvert.com এ .ico বানিয়ে
এই ফোল্ডারে `icon.ico` নামে রাখুন (না দিলে ডিফল্ট আইকন বসবে)।
