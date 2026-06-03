# Nexora VPN Real Engine Builder

این بسته، اپ نمایشی قبلی نیست. این Builder در GitHub Actions سورس رسمی **sing-box for Android / SFA** را می‌گیرد و APK واقعی مبتنی بر sing-box می‌سازد.

## کاری که باید بکنی

1. یک ریپوی جدید در GitHub بساز، مثلاً `nexora-sfa-builder`.
2. همه فایل‌های داخل این ZIP را داخل ریپو آپلود کن.
3. Push / Commit کن.
4. برو تب **Actions**.
5. Workflow با نام **Build Real Nexora VPN from SFA** را Run کن.
6. اگر Build سبز شد، پایین صفحه از بخش **Artifacts** فایل `NexoraVPN-real-SFA-apk` را دانلود کن.

## نکته مهم

این روش روی سورس رسمی SFA می‌سازد، پس موتور واقعی VPN دارد.  
اما اینکه پنل تو مستقیماً داخل رابط SFA مثل اپ اختصاصی سفارشی نمایش داده شود، مرحله بعدی است و باید دقیقاً روی ساختار سورس SFA همان روز Patch شود.

در این Builder فعلاً:
- اسم و برند Nexora اعمال می‌شود.
- لوگوی Nexora داخل منابع اپ قرار می‌گیرد.
- آدرس پنل تو در asset اپ ثبت می‌شود:
  `https://nexora-two-mu.vercel.app`
- APK واقعی SFA/sing-box ساخته می‌شود، نه connected فیک.

## پنل تو

API کانفیگ‌ها:
`https://nexora-two-mu.vercel.app/api/configs`

برای اتصال خودکار بدون دخالت کاربر، باید در مرحله بعدی روی سورس SFA بخش Profile/Remote Profile را Patch کنیم تا این URL را به‌صورت default اضافه کند.


## Fix included
این نسخه سورس SFA را با `--recursive` می‌گیرد تا submoduleهایی مثل `third_party/termux-app/terminal-view` هم دانلود شوند.


## Fix included: libbox.aar build
این نسخه قبل از Build اپ، سورس `SagerNet/sing-box` را می‌گیرد و با دستور:
`go run ./cmd/internal/build_libbox -target android`
فایل `libbox.aar` را می‌سازد و به مسیر `sfa/app/libs/libbox.aar` کپی می‌کند.


## Fix included: gomobile
این نسخه قبل از ساخت `libbox.aar` ابزارهای `gomobile` و `gobind` را نصب می‌کند و `gomobile init` را اجرا می‌کند.


## Fix included: SagerNet gomobile fork
این نسخه به‌جای `golang.org/x/mobile`، فورک `github.com/sagernet/gomobile` را نصب می‌کند، چون اسکریپت `build_libbox` sing-box به فلگ‌هایی مثل `-libname` نیاز دارد.
