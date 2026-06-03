package com.nexora.vpn.model

data class ServerConfig(
    val id: String,
    val title: String,
    val subtitle: String,
    val uri: String
)

object NexoraServers {
    val fallback = listOf(
        ServerConfig("HS1", "🇺🇸 HS1", "USA • Static IP • All networks", "vless://4b616b6f-6f6c-4e65-7773-075bbcddf9b8@20.103.221.187:443?encryption=none&security=tls&sni=shiny-space-funicular-6vg5q9px4r5rh4rww-443.app.github.dev&insecure=0&allowInsecure=0&type=ws&path=%2F#%40HS1"),
        ServerConfig("HS2", "🇺🇸 HS2", "USA • Static IP • All networks", "vless://4b616b6f-6f6c-4e65-7773-075bbcddf9b8@20.103.221.187:443?encryption=none&security=tls&sni=shiny-space-funicular-6vg5q9px4r5rh4rww-443.app.github.dev&insecure=0&allowInsecure=0&type=ws&path=%2F#%40HS2"),
        ServerConfig("HS3", "🇺🇸 HS3", "USA • Static IP • All networks", "vless://4b616b6f-6f6c-4e65-7773-075bbcddf9b8@20.103.221.187:443?encryption=none&security=tls&sni=shiny-space-funicular-6vg5q9px4r5rh4rww-443.app.github.dev&insecure=0&allowInsecure=0&type=ws&path=%2F#%40HS3")
    )
}
