package com.hizuyari

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val cBg = Color.parseColor("#050A24")
    private val cPanel = Color.parseColor("#0A0F38")
    private val cLine = Color.parseColor("#243B8F")
    private val cText = Color.parseColor("#FFFFFF")
    private val cBlueText = Color.parseColor("#3B9BFF")
    private val cOk = Color.parseColor("#16A34A")
    private val cStop = Color.parseColor("#E0301E")
    private val cSave = Color.parseColor("#2166E8")
    private val cTimer = Color.parseColor("#131B7D")
    private val cWarn = Color.parseColor("#8A5A00")
    private val cBad = Color.parseColor("#B01C1C")
    private val cGold = Color.parseColor("#FBBF24")

    private val timerBg by lazy { shape(cTimer, dp(28)) }

    private lateinit var speedTv: TextView
    private lateinit var timeTv: TextView
    private lateinit var statusTv: TextView
    private lateinit var hintTv: TextView
    private lateinit var btn: Button
    private lateinit var limitEt: EditText
    private lateinit var durEt: EditText
    private lateinit var testCb: CheckBox
    private lateinit var testBox: LinearLayout
    private lateinit var testTv: TextView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun prefs(): SharedPreferences = getSharedPreferences("ayar", MODE_PRIVATE)

    private fun lp(top: Int = 0): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        p.topMargin = top
        return p
    }

    private fun shape(fill: Int, radius: Int, stroke: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.cornerRadius = radius.toFloat()
        if (stroke != 0) g.setStroke(dp(2), stroke)
        return g
    }

    private fun label(s: String, size: Float, color: Int = cText, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.setTypeface(t.typeface, Typeface.BOLD)
        return t
    }

    private fun pill(text: String, color: Int): Button {
        val b = Button(this)
        b.text = text
        b.isAllCaps = false
        b.textSize = 20f
        b.setTypeface(b.typeface, Typeface.BOLD)
        b.setTextColor(Color.WHITE)
        b.background = shape(color, dp(40))
        b.minHeight = dp(60)
        b.minimumHeight = dp(60)
        b.stateListAnimator = null
        return b
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(18), dp(18), dp(18), dp(18))
        c.background = shape(cPanel, dp(28), cLine)
        return c
    }

    private fun numberField(value: Int): EditText {
        val e = EditText(this)
        e.inputType = InputType.TYPE_CLASS_NUMBER
        e.setText(value.toString())
        e.textSize = 22f
        e.setTypeface(e.typeface, Typeface.BOLD)
        e.setTextColor(cText)
        e.gravity = Gravity.CENTER
        e.background = shape(Color.parseColor("#02051A"), dp(20), cLine)
        e.setPadding(dp(12), dp(10), dp(12), dp(10))
        return e
    }

    private fun settingRow(title: String, field: EditText): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(label(title, 18f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r.addView(field, LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT))
        return r
    }

    private fun fmt(s: Int): String = "%02d:%02d".format(s / 60, s % 60)

    private fun hasLocation(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = cBg
        window.navigationBarColor = cBg

        val sv = ScrollView(this)
        sv.setBackgroundColor(cBg)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(24), dp(16), dp(32))
        sv.addView(root)
        setContentView(sv)

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(
            label("Hız Aşımı Sayacı", 30f, cBlueText, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        val badge = label("FK", 20f, cGold, true)
        badge.gravity = Gravity.CENTER
        badge.letterSpacing = 0.08f
        badge.setPadding(dp(14), dp(6), dp(14), dp(6))
        badge.background = shape(cPanel, dp(12), cGold)
        head.addView(
            badge,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(head, lp())

        // Kart 1: hız ve sayaç
        val c1 = card()
        c1.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(c1, lp(dp(16)))

        c1.addView(label("Anlık GPS hızı", 18f).apply { gravity = Gravity.CENTER }, lp())
        speedTv = label("0", 96f, Color.WHITE, true)
        speedTv.gravity = Gravity.CENTER
        c1.addView(speedTv, lp())
        c1.addView(label("km/sa", 28f, cBlueText).apply { gravity = Gravity.CENTER }, lp())

        val tb = LinearLayout(this)
        tb.orientation = LinearLayout.VERTICAL
        tb.gravity = Gravity.CENTER
        tb.setPadding(dp(16), dp(20), dp(16), dp(20))
        tb.background = timerBg
        timeTv = label("00:00 / 00:45", 34f, Color.WHITE, true)
        timeTv.gravity = Gravity.CENTER
        timeTv.isSingleLine = true
        statusTv = label("", 20f)
        statusTv.gravity = Gravity.CENTER
        tb.addView(timeTv, lp())
        tb.addView(statusTv, lp())
        c1.addView(tb, lp(dp(16)))

        btn = pill("Takibi başlat", cOk)
        btn.setOnClickListener { toggle() }
        c1.addView(btn, lp(dp(16)))

        hintTv = label("", 16f)
        hintTv.gravity = Gravity.CENTER
        c1.addView(hintTv, lp(dp(12)))

        // Kart 2: ayarlar
        val p = prefs()
        val c2 = card()
        root.addView(c2, lp(dp(16)))
        limitEt = numberField(p.getInt("limit", 90))
        durEt = numberField(p.getInt("dur", 45))
        c2.addView(settingRow("Hız sınırı (km/sa)", limitEt), lp())
        c2.addView(settingRow("Uyarı süresi (saniye)", durEt), lp(dp(12)))
        val saveBtn = pill("Kaydet", cSave)
        saveBtn.setOnClickListener { save() }
        c2.addView(saveBtn, lp(dp(16)))

        // Kart 3: test modu
        val c3 = card()
        root.addView(c3, lp(dp(16)))
        testCb = CheckBox(this)
        testCb.text = "Test modu"
        testCb.textSize = 18f
        testCb.setTextColor(cText)
        testCb.buttonTintList = ColorStateList.valueOf(cBlueText)
        c3.addView(testCb, lp())

        testBox = LinearLayout(this)
        testBox.orientation = LinearLayout.VERTICAL
        testBox.visibility = View.GONE
        testTv = label("Test hızı: 0 km/sa", 17f)
        val sb = SeekBar(this)
        sb.max = 200
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                SpeedService.testSpeed = v.toFloat()
                testTv.text = "Test hızı: $v km/sa"
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        testBox.addView(testTv, lp(dp(8)))
        testBox.addView(sb, lp(dp(8)))
        c3.addView(testBox, lp())

        testCb.setOnCheckedChangeListener { _, on ->
            SpeedService.testMode = on
            testBox.visibility = if (on) View.VISIBLE else View.GONE
        }

    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val v = currentFocus ?: limitEt
        imm.hideSoftInputFromWindow(v.windowToken, 0)
        limitEt.clearFocus()
        durEt.clearFocus()
    }

    private fun save() {
        hideKeyboard()
        val l = limitEt.text.toString().toIntOrNull()
        val d = durEt.text.toString().toIntOrNull()
        if (l == null || d == null || l !in 10..250 || d !in 1..600) {
            Toast.makeText(this, "Geçersiz değer", Toast.LENGTH_SHORT).show()
            return
        }
        prefs().edit().putInt("limit", l).putInt("dur", d).apply()
        Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun toggle() {
        if (SpeedService.running) {
            stopService(Intent(this, SpeedService::class.java))
        } else if (!hasLocation()) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1
            )
        } else {
            startForegroundService(Intent(this, SpeedService::class.java))
        }
        window.decorView.postDelayed({ refresh() }, 300)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (hasLocation()) {
            startForegroundService(Intent(this, SpeedService::class.java))
            window.decorView.postDelayed({ refresh() }, 300)
        } else {
            Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        testCb.isChecked = SpeedService.testMode
        SpeedService.listener = { runOnUiThread { refresh() } }
        refresh()
    }

    override fun onPause() {
        SpeedService.listener = null
        super.onPause()
    }

    private fun refresh() {
        val on = SpeedService.running
        val p = prefs()
        val limit = p.getInt("limit", 90)
        val dur = p.getInt("dur", 45)

        speedTv.text = if (on) SpeedService.speedKmh.toInt().toString() else "0"
        timeTv.text = "${fmt(SpeedService.overSec)} / ${fmt(dur)}"

        val (bg, msg) = when {
            !on -> Pair(cTimer, "Takip kapalı")
            SpeedService.alarming -> Pair(cBad, "ALARM! Hızını düşür")
            SpeedService.speedKmh > limit -> Pair(cWarn, "Hız sınırın üstünde")
            !SpeedService.testMode && !SpeedService.gpsOk -> Pair(cTimer, "GPS sinyali bekleniyor...")
            else -> Pair(cTimer, "Hız sınırın altında")
        }
        timerBg.setColor(bg)
        statusTv.text = msg

        btn.text = if (on) "Takibi durdur" else "Takibi başlat"
        btn.background = shape(if (on) cStop else cOk, dp(40))
        hintTv.text = if (on) "Takip açık. Ekran kapansa da çalışır." else "Takip kapalı."
    }
}
