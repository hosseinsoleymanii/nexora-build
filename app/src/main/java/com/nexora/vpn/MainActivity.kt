package com.nexora.vpn

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import kotlin.concurrent.thread

data class ServerConfig(
    val name: String,
    val subtitle: String,
    val config: String
)

class MainActivity : Activity() {

    private val panelApi = "https://nexora-two-mu.vercel.app/api/configs"
    private lateinit var listContainer: LinearLayout
    private lateinit var statusText: TextView
    private val servers = mutableListOf<ServerConfig>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        fetchConfigs()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#07111F"))
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val logo = ImageView(this).apply {
            val resId = resources.getIdentifier("nexora_logo", "drawable", packageName)
            if (resId != 0) {
                setImageResource(resId)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        root.addView(logo, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(96)
        ).apply {
            setMargins(0, dp(8), 0, dp(10))
        })

        val title = TextView(this).apply {
            text = "Nexora VPN"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(6))
        }

        val sub = TextView(this).apply {
            text = "کانفیگ‌ها از پنل نکسورا دریافت می‌شوند"
            textSize = 14f
            setTextColor(Color.parseColor("#9EB6D2"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(18))
        }

        val refresh = Button(this).apply {
            text = "به‌روزرسانی سرورها"
            setOnClickListener { fetchConfigs() }
        }

        statusText = TextView(this).apply {
            text = "در حال دریافت کانفیگ‌ها..."
            textSize = 14f
            setTextColor(Color.parseColor("#9EB6D2"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
        }

        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)

        root.addView(title)
        root.addView(sub)
        root.addView(refresh)
        root.addView(statusText)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        setContentView(root)
    }

    private fun fetchConfigs() {
        statusText.text = "در حال اتصال به پنل..."
        listContainer.removeAllViews()

        thread {
            try {
                val conn = URL(panelApi).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Cache-Control", "no-cache")

                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                }

                if (code !in 200..299) throw Exception(body)

                val parsed = parseConfigsFlexible(body)

                runOnUiThread {
                    servers.clear()
                    servers.addAll(parsed)
                    renderServers()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "خطا در دریافت کانفیگ‌ها"
                    Toast.makeText(this, "اتصال به پنل انجام نشد", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseConfigsFlexible(body: String): List<ServerConfig> {
        val result = mutableListOf<ServerConfig>()
        val trimmed = body.trim()

        if (trimmed.contains("vless://") && !trimmed.startsWith("[") && !trimmed.startsWith("{")) {
            trimmed.lines()
                .map { it.trim() }
                .filter { it.startsWith("vless://") }
                .forEachIndexed { index, line ->
                    result.add(ServerConfig(extractNameFromVless(line, "سرور ${index + 1}"), "", line))
                }
            return result
        }

        val arr: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val obj = JSONObject(trimmed)
                when {
                    obj.has("servers") -> obj.getJSONArray("servers")
                    obj.has("configs") -> obj.getJSONArray("configs")
                    obj.has("data") -> obj.getJSONArray("data")
                    obj.has("items") -> obj.getJSONArray("items")
                    else -> JSONArray()
                }
            }
        }

        for (i in 0 until arr.length()) {
            val value = arr.get(i)

            if (value is String) {
                val config = value.trim()
                if (config.startsWith("vless://")) {
                    result.add(ServerConfig(extractNameFromVless(config, "سرور ${i + 1}"), "", config))
                }
                continue
            }

            val item = arr.getJSONObject(i)
            if (item.optBoolean("enabled", true) == false) continue

            val config = when {
                item.has("uri") -> item.optString("uri")
                item.has("config") -> item.optString("config")
                item.has("url") -> item.optString("url")
                item.has("link") -> item.optString("link")
                item.has("vless") -> item.optString("vless")
                item.has("value") -> item.optString("value")
                else -> ""
            }.trim()

            if (!config.startsWith("vless://")) continue

            val name = item.optString(
                "title",
                item.optString(
                    "name",
                    item.optString("id", extractNameFromVless(config, "سرور ${i + 1}"))
                )
            )

            val subtitle = item.optString("subtitle", item.optString("remark", ""))

            result.add(ServerConfig(name, subtitle, config))
        }

        return result
    }

    private fun extractNameFromVless(config: String, fallback: String): String {
        return try {
            val idx = config.indexOf("#")
            if (idx >= 0 && idx < config.length - 1) {
                URLDecoder.decode(config.substring(idx + 1), "UTF-8")
            } else fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun renderServers() {
        listContainer.removeAllViews()

        if (servers.isEmpty()) {
            statusText.text = "هیچ کانفیگ فعالی در پنل پیدا نشد."
            return
        }

        statusText.text = "${servers.size} سرور دریافت شد"

        servers.forEach { server ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                setBackgroundColor(Color.parseColor("#101D2F"))
            }

            val title = TextView(this).apply {
                text = server.name
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.RIGHT
            }

            val subtitle = TextView(this).apply {
                text = if (server.subtitle.isBlank()) "آماده کپی" else server.subtitle
                textSize = 13f
                setTextColor(Color.parseColor("#9EB6D2"))
                gravity = Gravity.RIGHT
                setPadding(0, dp(4), 0, dp(10))
            }

            val copyBtn = Button(this).apply {
                text = "کپی کانفیگ"
                setOnClickListener { copyConfig(server.config) }
            }

            card.addView(title)
            card.addView(subtitle)
            card.addView(copyBtn)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(12))
            }

            listContainer.addView(card, params)
        }
    }

    private fun copyConfig(config: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Nexora VPN Config", config))
        Toast.makeText(this, "کانفیگ کپی شد", Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
