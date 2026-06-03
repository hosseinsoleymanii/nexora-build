# Nexora VPN Panel Button Fix

The admin panel was converted to a proper Next.js Client Component.
Previously the buttons used inline `onclick` strings inside a Server Component, so React/Next.js did not attach the handlers reliably.

Deploy the `backend` folder to Vercel again, then open `/admin`.
