package com.personal.timetracker.ui.support

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.personal.timetracker.license.LicenseStore
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.FigmaUi
import com.personal.timetracker.util.NotifHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * چت شبیه پیام‌رسان: لیست گفتگوها → باز کردن ترد → ارسال / حذف پیام.
 */
class SupportFragment : Fragment() {
    private lateinit var root: LinearLayout
    private lateinit var listPane: LinearLayout
    private lateinit var chatPane: LinearLayout
    private lateinit var msgBox: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var chatTitle: TextView

    /** null = لیست گفتگوها | "support" = پشتیبانی | peerId = مستقیم */
    private var openId: String? = null
    private var lastSig = ""
    private var knownUnread = 0

    private val handler = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() {
            if (!isAdded) return
            if (openId == null) loadConversations(silent = true)
            else loadMessages(silent = true)
            handler.postDelayed(this, 4000)
        }
    }

    private fun dark() = (activity as? MainActivity)?.isDark == true
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val isDark = dark()
        root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(FigmaUi.bg(isDark))
        }
        root.addView(FigmaUi.screenHeader(ctx, "پیام‌ها", "پشتیبانی و همکاران", "＋") {
            showNewChatPicker()
        })

        listPane = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
        }
        chatPane = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
        }

        // list
        val listScroll = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
        }
        val listBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            tag = "listBox"
        }
        listScroll.addView(listBox)
        listPane.addView(listScroll)

        // chat header
        val ch = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(FigmaUi.card(isDark))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            elevation = 4f
        }
        ch.addView(TextView(ctx).apply {
            text = "‹"
            textSize = 22f
            setTextColor(FigmaUi.PRIMARY)
            setPadding(dp(8), 0, dp(12), 0)
            setOnClickListener { showList() }
        })
        chatTitle = TextView(ctx).apply {
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(FigmaUi.text(isDark))
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
            gravity = Gravity.END
        }
        ch.addView(chatTitle)
        ch.addView(TextView(ctx).apply {
            text = "🗑"
            textSize = 18f
            setPadding(dp(12), 0, 0, 0)
            setOnClickListener { confirmClearThread() }
        })
        chatPane.addView(ch)

        scroll = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
        }
        msgBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        scroll.addView(msgBox)
        chatPane.addView(scroll)

        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(FigmaUi.card(isDark))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            elevation = 8f
        }
        input = EditText(ctx).apply {
            hint = "پیام…"
            maxLines = 4
            textSize = 14f
            setTextColor(FigmaUi.text(isDark))
            setHintTextColor(FigmaUi.muted(isDark))
            background = FigmaUi.rounded(if (isDark) 0xFF243447.toInt() else 0xFFF5F7FA.toInt(), 14f, ctx)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        }
        bar.addView(input)
        bar.addView(MaterialButton(ctx).apply {
            text = "ارسال"
            setBackgroundColor(FigmaUi.PRIMARY)
            setTextColor(Color.WHITE)
            cornerRadius = dp(12)
            setOnClickListener { send() }
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = dp(8) }
        })
        chatPane.addView(bar)

        root.addView(listPane)
        root.addView(chatPane)
        showList()
        return root
    }

    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    private fun dp(v: Int) = FigmaUi.dp(requireContext(), v)

    override fun onResume() {
        super.onResume()
        handler.post(poll)
    }

    override fun onPause() {
        handler.removeCallbacks(poll)
        super.onPause()
    }

    private fun showList() {
        openId = null
        lastSig = ""
        listPane.visibility = View.VISIBLE
        chatPane.visibility = View.GONE
        loadConversations(silent = false)
    }

    private fun openThread(id: String, title: String) {
        openId = id
        lastSig = ""
        chatTitle.text = title
        listPane.visibility = View.GONE
        chatPane.visibility = View.VISIBLE
        loadMessages(silent = false)
    }

    private fun listBox(): LinearLayout =
        listPane.findViewWithTag("listBox") as LinearLayout

    private fun loadConversations(silent: Boolean) {
        lifecycleScope.launch {
            val ctx = requireContext()
            val key = LicenseStore.licenseKey(ctx) ?: return@launch
            val deviceId = LicenseStore.deviceId(ctx)
            val base = LicenseStore.baseUrl(ctx).trimEnd('/')
            try {
                val body = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder()
                            .url("$base/api/v1/chat?licenseKey=$key&deviceId=$deviceId&mode=conversations")
                            .get().build()
                    ).execute().use { it.body?.string().orEmpty() }
                }
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) {
                    if (!silent) Toast.makeText(ctx, json.optString("error"), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val arr = json.optJSONArray("conversations") ?: JSONArray()
                var unreadSum = 0
                for (i in 0 until arr.length()) unreadSum += arr.getJSONObject(i).optInt("unread")
                if (silent && unreadSum > knownUnread) {
                    NotifHelper.show(ctx, "پیام جدید", "شما ${unreadSum - knownUnread} پیام خوانده‌نشده دارید", 7101)
                }
                knownUnread = unreadSum

                val box = listBox()
                box.removeAllViews()
                if (arr.length() == 0) {
                    box.addView(TextView(ctx).apply {
                        text = "گفتگویی نیست — ＋ برای شروع با همکار"
                        setTextColor(FigmaUi.muted(dark()))
                        gravity = Gravity.CENTER
                        setPadding(0, dp(40), 0, dp(40))
                    })
                }
                // همیشه پشتیبانی در بالا
                var hasSupport = false
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    if (c.optString("id") == "support") {
                        hasSupport = true
                        box.addView(conversationRow(c))
                    }
                }
                if (!hasSupport) {
                    box.addView(conversationRow(JSONObject()
                        .put("id", "support")
                        .put("type", "support")
                        .put("title", "پشتیبانی")
                        .put("lastMessage", "گفتگو با مدیر")
                        .put("unread", 0)))
                }
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    if (c.optString("id") != "support") box.addView(conversationRow(c))
                }
            } catch (e: Exception) {
                if (!silent) Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun conversationRow(c: JSONObject): View {
        val ctx = requireContext()
        val isDark = dark()
        val id = c.optString("id")
        val title = c.optString("title", "گفتگو")
        val last = c.optString("lastMessage", "")
        val unread = c.optInt("unread")
        val type = c.optString("type")
        val card = FigmaUi.whiteCard(ctx, isDark)
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setOnClickListener { openThread(id, if (type == "support") "پشتیبانی" else title) }
            setOnLongClickListener {
                if (id != "support") confirmClearThread(id)
                true
            }
        }
        if (unread > 0) {
            row.addView(TextView(ctx).apply {
                text = unread.toString()
                textSize = 11f
                setTextColor(Color.WHITE)
                background = FigmaUi.oval(FigmaUi.PRIMARY)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) }
            })
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        }
        col.addView(TextView(ctx).apply {
            text = if (type == "support") "🛠 پشتیبانی" else "👤 $title"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(FigmaUi.text(isDark))
            gravity = Gravity.END
        })
        col.addView(TextView(ctx).apply {
            text = last.ifBlank { "—" }
            textSize = 12f
            setTextColor(FigmaUi.muted(isDark))
            gravity = Gravity.END
            maxLines = 1
        })
        row.addView(col)
        card.addView(row)
        card.layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply {
            bottomMargin = dp(8)
        }
        return card
    }

    private fun loadMessages(silent: Boolean) {
        val thread = openId ?: return
        lifecycleScope.launch {
            val ctx = requireContext()
            val key = LicenseStore.licenseKey(ctx) ?: return@launch
            val deviceId = LicenseStore.deviceId(ctx)
            val base = LicenseStore.baseUrl(ctx).trimEnd('/')
            try {
                val url = if (thread == "support") {
                    "$base/api/v1/chat?licenseKey=$key&deviceId=$deviceId&kind=support"
                } else {
                    "$base/api/v1/chat?licenseKey=$key&deviceId=$deviceId&kind=direct&peerId=$thread"
                }
                val body = withContext(Dispatchers.IO) {
                    client.newCall(Request.Builder().url(url).get().build())
                        .execute().use { it.body?.string().orEmpty() }
                }
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) {
                    if (!silent) Toast.makeText(ctx, json.optString("error"), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val arr = json.optJSONArray("messages") ?: JSONArray()
                val sig = "${arr.length()}_${arr.optJSONObject(arr.length() - 1)?.optString("id")}"
                if (silent && sig == lastSig) return@launch
                val hadNew = lastSig.isNotEmpty() && sig != lastSig
                lastSig = sig
                msgBox.removeAllViews()
                if (arr.length() == 0) {
                    msgBox.addView(TextView(ctx).apply {
                        text = "هنوز پیامی نیست"
                        setTextColor(FigmaUi.muted(dark()))
                        gravity = Gravity.CENTER
                        setPadding(0, dp(32), 0, dp(32))
                    })
                }
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    msgBox.addView(bubble(m))
                }
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                if (hadNew && silent) {
                    val last = arr.optJSONObject(arr.length() - 1)
                    if (last != null && !last.optBoolean("mine")) {
                        NotifHelper.show(
                            ctx,
                            chatTitle.text.toString(),
                            last.optString("body"),
                            7102
                        )
                    }
                }
            } catch (e: Exception) {
                if (!silent) Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bubble(m: JSONObject): View {
        val ctx = requireContext()
        val isDark = dark()
        val mine = m.optBoolean("mine")
        val admin = m.optString("sender") == "admin" || m.optString("kind") == "broadcast"
        val bg = when {
            admin && !mine -> FigmaUi.PRIMARY
            mine -> if (isDark) 0xFF1A3A5C.toInt() else 0xFFE3F2FD.toInt()
            else -> if (isDark) 0xFF243447.toInt() else 0xFFF3F4F6.toInt()
        }
        val fg = if (admin && !mine) Color.WHITE else FigmaUi.text(isDark)
        val id = m.optString("id")
        return TextView(ctx).apply {
            text = m.optString("body")
            textSize = 14f
            setTextColor(fg)
            background = FigmaUi.rounded(bg, 16f, ctx)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                (ctx.resources.displayMetrics.widthPixels * 0.75).toInt(), WRAP
            ).apply {
                gravity = if (mine) Gravity.END else Gravity.START
                bottomMargin = dp(8)
            }
            setOnLongClickListener {
                if (!mine) {
                    Toast.makeText(ctx, "فقط پیام‌های خودتان را می‌توانید حذف کنید", Toast.LENGTH_SHORT).show()
                    return@setOnLongClickListener true
                }
                AlertDialog.Builder(ctx)
                    .setTitle("حذف پیام")
                    .setMessage("این پیام حذف شود؟")
                    .setPositiveButton("حذف") { _, _ -> deleteMessage(id) }
                    .setNegativeButton("انصراف", null)
                    .show()
                true
            }
        }
    }

    private fun send() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty() || openId == null) return
        lifecycleScope.launch {
            val ctx = requireContext()
            val key = LicenseStore.licenseKey(ctx) ?: return@launch
            val deviceId = LicenseStore.deviceId(ctx)
            val base = LicenseStore.baseUrl(ctx).trimEnd('/')
            try {
                val payload = JSONObject()
                    .put("licenseKey", key)
                    .put("deviceId", deviceId)
                    .put("body", text)
                if (openId != "support") {
                    payload.put("peerId", openId)
                    payload.put("kind", "direct")
                } else {
                    payload.put("kind", "support")
                }
                val resp = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url("$base/api/v1/chat")
                            .post(payload.toString().toRequestBody("application/json".toMediaType()))
                            .build()
                    ).execute().use { it.body?.string().orEmpty() }
                }
                if (JSONObject(resp).optBoolean("ok") != true) {
                    Toast.makeText(ctx, "ارسال ناموفق", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                input.setText("")
                lastSig = ""
                loadMessages(silent = false)
            } catch (e: Exception) {
                Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteMessage(id: String) {
        lifecycleScope.launch {
            val ctx = requireContext()
            val key = LicenseStore.licenseKey(ctx) ?: return@launch
            val deviceId = LicenseStore.deviceId(ctx)
            val base = LicenseStore.baseUrl(ctx).trimEnd('/')
            withContext(Dispatchers.IO) {
                val payload = JSONObject()
                    .put("licenseKey", key)
                    .put("deviceId", deviceId)
                    .put("messageId", id)
                client.newCall(
                    Request.Builder().url("$base/api/v1/chat")
                        .delete(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                ).execute().close()
            }
            lastSig = ""
            loadMessages(silent = false)
        }
    }

    private fun confirmClearThread(id: String? = openId) {
        val thread = id ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("حذف گفتگو")
            .setMessage("کل این گفتگو پاک شود؟")
            .setPositiveButton("حذف") { _, _ -> clearThread(thread) }
            .setNegativeButton("انصراف", null)
            .show()
    }

    private fun clearThread(thread: String) {
        lifecycleScope.launch {
            val ctx = requireContext()
            val key = LicenseStore.licenseKey(ctx) ?: return@launch
            val deviceId = LicenseStore.deviceId(ctx)
            val base = LicenseStore.baseUrl(ctx).trimEnd('/')
            withContext(Dispatchers.IO) {
                val payload = JSONObject()
                    .put("licenseKey", key)
                    .put("deviceId", deviceId)
                if (thread != "support") payload.put("peerId", thread)
                client.newCall(
                    Request.Builder().url("$base/api/v1/chat")
                        .delete(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                ).execute().close()
            }
            if (openId != null) showList() else loadConversations(false)
        }
    }

    private fun showNewChatPicker() {
        lifecycleScope.launch {
            val ctx = requireContext()
            val key = LicenseStore.licenseKey(ctx) ?: return@launch
            val deviceId = LicenseStore.deviceId(ctx)
            val base = LicenseStore.baseUrl(ctx).trimEnd('/')
            try {
                val body = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder()
                            .url("$base/api/v1/chat/peers?licenseKey=$key&deviceId=$deviceId")
                            .get().build()
                    ).execute().use { it.body?.string().orEmpty() }
                }
                val json = JSONObject(body)
                val arr = json.optJSONArray("peers") ?: JSONArray()
                val names = mutableListOf<String>()
                val ids = mutableListOf<String>()
                names.add("🛠 پشتیبانی")
                ids.add("support")
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    names.add(p.optString("name", "همکار"))
                    ids.add(p.optString("id"))
                }
                AlertDialog.Builder(ctx)
                    .setTitle("گفتگوی جدید")
                    .setItems(names.toTypedArray()) { _, which ->
                        openThread(ids[which], names[which].removePrefix("🛠 ").removePrefix("👤 "))
                    }
                    .setNegativeButton("انصراف", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
