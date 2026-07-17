# ধাপ ২: ফোনের APK বিল্ড (তিনটা উপায়)

## উপায় A — GitHub Actions (সবচেয়ে সহজ, সুপারিশ) ✅
কম্পিউটারে কিছু ইনস্টল লাগে না, ফোনের ব্রাউজারেই হয়।

**একবারের প্রস্তুতি (১ মিনিট):**
রিপোতে `ci/build-apk.yml` ফাইলটা ওয়েবে খুলুন → পেন্সিল (Edit) আইকন →
ফাইলনেমের ঘরে লিখুন `.github/workflows/build-apk.yml` → **Commit changes**।
(এরপর Actions ট্যাব কাজ করবে।)

**তারপর বিল্ড:**
1. ফোনে github.com খুলে লগইন → রিপো **onlylipu-cloud** খুলুন।
2. উপরে **Actions** ট্যাব → বামে **Build OnlyLipu Cloud APK** →
   ডানে **Run workflow** বাটন → সবুজ **Run workflow**।
3. ৫-৮ মিনিট অপেক্ষা → বিল্ড সফল হলে ওই পেজে নিচে
   **OnlyLipuCloud-debug-apk** আর্টিফ্যাক্ট → ট্যাপ করে zip ডাউনলোড।
4. zip-এর ভেতরের `app-debug.apk` ফোনে ইনস্টল করুন
   (চাইলে Settings → Install unknown apps-এ অনুমতি দিন)।

> সার্ভার URL বদলাতে: রিপো → Settings → Secrets and variables →
> Actions → **Variables** → `SERVER_URL` = `https://আপনার-ডোমেইন`

## উপায় B — ল্যাপটপে Android Studio
1. https://developer.android.com/studio ডাউনলোড করে ইনস্টল।
2. GitHub রিপো → **Code → Download ZIP** → আনজিপ।
3. Android Studio → **Open** → `android-app` ফোল্ডার সিলেক্ট।
4. `android-app` ফোল্ডারে `local.properties` নামে ফাইল বানান:
   ```properties
   sdk.dir=C:\\Users\\আপনার-নাম\\AppData\\Local\\Android\\Sdk
   ONLYLIPU_SERVER_URL=https://149.28.20.169.sslip.io
   ```
5. Gradle sync শেষ হলে মেনু: **Build → Build App Bundle(s)/APK(s) → Build APK(s)**।
6. `app/build/outputs/apk/debug/app-debug.apk` ফোনে পাঠিয়ে ইনস্টল।

**Release APK (প্লে-স্টোর-মানের):**
Build → **Generate Signed App Bundle/APK** → APK → Create new keystore
(keystore-এর পাসওয়ার্ড ভুলবেন না — কাগজে লিখে রাখুন)।

## উপায় C — ফোনেই Termux দিয়ে (অ্যাডভান্সড)
> ধীর, অনেক ডাউনলোড লাগে (~3 GB)। শুধু অভিজ্ঞ হলে।
```bash
pkg install openjdk-17 git wget unzip
git clone https://github.com/deadlife2651-hash/onlylipu-cloud
cd onlylipu-cloud/android-app
# cmdline-tools নামিয়ে sdkmanager দিয়ে platform 35 ও build-tools বসিয়ে
gradle assembleDebug
```
বিস্তারিত সহায়তা লাগলে জানাবেন — ধাপে ধাপে করে দেবো।

## ইনস্টলের পর
অ্যাপ খুলুন → Admin panel-এর **ইউজারনেম/পাসওয়ার্ড** দিয়ে সাইন-ইন
(`/root/onlylipu-credentials.txt`-এ আছে) → Dashboard-এ
Cloud Computer / Cloud Android কার্ডে ট্যাপ।
