package com.nexora.vpn.network

import com.nexora.vpn.model.NexoraServers
import com.nexora.vpn.model.ServerConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ConfigRepository {
    fun fetchServers(): List<ServerConfig> {
        val conn = URL(BackendConfig.CONFIGS_ENDPOINT).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 12000
        conn.readTimeout = 12000
        conn.setRequestProperty("Accept", "application/json")

        return try {
            val code = conn.responseCode
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            }

            if (code !in 200..299) throw IllegalStateException(text)

            parseServers(text).ifEmpty { NexoraServers.fallback }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseServers(text: String): List<ServerConfig> {
        val trimmed = text.trim()
        val arr: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val obj = JSONObject(trimmed)
                when {
                    obj.has("servers") -> obj.getJSONArray("servers")
                    obj.has("configs") -> obj.getJSONArray("configs")
                    obj.has("data") -> obj.getJSONArray("data")
                    else -> JSONArray()
                }
            }
        }

        val out = mutableListOf<ServerConfig>()

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)

            val uri = when {
                o.has("uri") -> o.optString("uri")
                o.has("config") -> o.optString("config")
                o.has("url") -> o.optString("url")
                o.has("link") -> o.optString("link")
                o.has("vless") -> o.optString("vless")
                else -> ""
            }.trim()

            if (!uri.startsWith("vless://")) continue

            val id = o.optString("id", "S${i + 1}")
            val title = o.optString(
                "title",
                o.optString("name", o.optString("remark", "Server ${i + 1}"))
            )
            val subtitle = o.optString(
                "subtitle",
                o.optString("country", "All networks")
            )

            out.add(
                ServerConfig(
                    id = id,
                    title = title,
                    subtitle = subtitle,
                    uri = uri
                )
            )
        }

        return out
    }
}
