# Nexora VPN Panel - Redis Version

این نسخه پنل به جای `KV_REST_API_URL` و `KV_REST_API_TOKEN` مستقیم از `REDIS_URL` استفاده می‌کند.

در Vercel فقط این Environment Variables لازم است:

- `ADMIN_TOKEN`
- `REDIS_URL`

بعد از جایگزین‌کردن فایل‌ها در GitHub، در Vercel حتماً Redeploy بزنید.
