package com.nexora.vpn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class ServerConfig(
    val name: String,
    val country: String,
    val remark: String,
    val config: String
)

class MainActivity : AppCompatActivity() {

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

        val title = TextView(this).apply {
            text = "نکسورا وی‌پی‌ان"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(6))
        }

        val sub = TextView(this).apply {
            text = "کانفیگ‌ها از پنل شما دریافت می‌شوند"
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
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.setRequestProperty("Accept", "application/json")

                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                }

                if (code !in 200..299) throw Exception(body)

                val parsed = parseConfigs(body)

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

    private fun parseConfigs(body: String): List<ServerConfig> {
        val result = mutableListOf<ServerConfig>()
        val trimmed = body.trim()

        val arr: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val obj = JSONObject(trimmed)
                when {
                    obj.has("configs") -> obj.getJSONArray("configs")
                    obj.has("servers") -> obj.getJSONArray("servers")
                    obj.has("data") -> obj.getJSONArray("data")
                    else -> JSONArray()
                }
            }
        }

        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val config = when {
                item.has("config") -> item.optString("config")
                item.has("url") -> item.optString("url")
                item.has("link") -> item.optString("link")
                item.has("vless") -> item.optString("vless")
                else -> ""
            }.trim()

            if (!config.startsWith("vless://")) continue

            val name = item.optString(
                "name",
                item.optString(
                    "title",
                    item.optString("remark", "سرور ${i + 1}")
                )
            )
            val country = item.optString("country", "🌐")
            val remark = item.optString("remark", name)

            result.add(ServerConfig(name, country, remark, config))
        }

        return result
    }

    private fun renderServers() {
        listContainer.removeAllViews()

        if (servers.isEmpty()) {
            statusText.text = "هیچ کانفیگ VLESS در پنل پیدا نشد."
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
                text = "${server.country} ${server.name}"
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.RIGHT
            }

            val remark = TextView(this).apply {
                text = server.remark
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
            card.addView(remark)
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
        cm.setPrimaryClip(ClipData.newPlainText("کانفیگ Nexora VPN", config))
        Toast.makeText(this, "کانفیگ کپی شد", Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
