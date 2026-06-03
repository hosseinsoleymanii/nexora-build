package com.nexora.vpn.network

object BackendConfig {
    /**
     * بعد از Deploy روی Vercel فقط این آدرس را عوض کن و دوباره APK بگیر.
     * مثال: https://nexora-vpn-panel.vercel.app
     */
    const val BASE_URL = "https://teachershow.ir"
    const val CONFIGS_ENDPOINT = "$BASE_URL/api/configs"
}
