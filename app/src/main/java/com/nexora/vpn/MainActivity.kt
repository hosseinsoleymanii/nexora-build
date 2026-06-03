package com.nexora.vpn

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import com.nexora.vpn.model.NexoraServers
import com.nexora.vpn.model.ServerConfig
import com.nexora.vpn.network.ConfigRepository
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private var servers: List<ServerConfig> = NexoraServers.fallback
    private var selected: ServerConfig = servers.first()
    private lateinit var status: TextView
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refreshServers()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(0xFF1D1F25.toInt())
        }

        val logo = ImageView(this).apply {
            setImageResource(resources.getIdentifier("nexora_logo", "drawable", packageName))
            adjustViewBounds = true
            maxHeight = dp(180)
        }

        val title = TextView(this).apply {
            text = "Nexora VPN"
            textSize = 30f
            setTextColor(0xFFF4F8FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(4))
        }

        val subtitle = TextView(this).apply {
            text = "انتقال کانفیگ‌ها به v2rayNG / Hiddify / NekoBox"
            textSize = 14f
            setTextColor(0xFFA9B4C4.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }

        status = TextView(this).apply {
            text = "Loading servers..."
            textSize = 15f
            setTextColor(0xFFA9B4C4.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }

        val refresh = Button(this).apply {
            text = "REFRESH FROM PANEL"
            setOnClickListener { refreshServers() }
        }

        val open = Button(this).apply {
            text = "OPEN / IMPORT SELECTED"
            textSize = 17f
            setOnClickListener { openConfig(selected.uri) }
        }

        val copy = Button(this).apply {
            text = "COPY SELECTED CONFIG"
            setOnClickListener { copyConfig(selected.uri) }
        }

        val share = Button(this).apply {
            text = "SHARE SELECTED CONFIG"
            setOnClickListener { shareConfig(selected.uri) }
        }

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)

        root.addView(logo)
        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(refresh)
        root.addView(open)
        root.addView(copy)
        root.addView(share)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        setContentView(root)
        renderServers()
    }

    private fun renderServers() {
        list.removeAllViews()

        servers.forEach { server ->
            val btn = Button(this).apply {
                text = if (server == selected) {
                    "✓ ${server.title}\n${server.subtitle}"
                } else {
                    "${server.title}\n${server.subtitle}"
                }
                textSize = 15f
                setAllCaps(false)
                setOnClickListener {
                    selected = server
                    status.text = "Selected: ${server.title}"
                    renderServers()
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }

            list.addView(btn, params)
        }
    }

    private fun refreshServers() {
        status.text = "Updating from Vercel panel..."
        thread {
            try {
                val fresh = ConfigRepository.fetchServers()
                runOnUiThread {
                    servers = fresh
                    selected = servers.first()
                    renderServers()
                    status.text = "${servers.size} server(s) loaded from panel"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    servers = NexoraServers.fallback
                    selected = servers.first()
                    renderServers()
                    status.text = "Panel unavailable; fallback servers loaded"
                    Toast.makeText(this, e.message ?: "Panel error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openConfig(config: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "برنامه مناسب پیدا نشد. v2rayNG، Hiddify یا NekoBox نصب کن.",
                Toast.LENGTH_LONG
            ).show()
            shareConfig(config)
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Open failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareConfig(config: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, config)
        }
        startActivity(Intent.createChooser(intent, "Send config to VPN app"))
    }

    private fun copyConfig(config: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Nexora VPN Config", config))
        Toast.makeText(this, "Config copied", Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
