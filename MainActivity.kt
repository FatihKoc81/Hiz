package com.hizuyari

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val timerBg = GradientDrawable()

    // Renkler – ikinci ekran görüntüsüne uygun
    private val cBg = Color.parseColor("#0B0E14")
    private val cPanel = Color.parseColor("#12161F")
    private val cLine = Color.parseColor("#1E2430")
    private val cText = Color.parseColor("#E8EAED")
    private val cMuted = Color.parseColor("#8B93A0")
    private val cBlueText = Color.parseColor("#60A5FA")
    private val cOk = Color.parseColor("#22C55E")       // Yeşil buton
    private val cStop = Color.parseColor("#3A3F45")
    private val cWarn = Color.parseColor("#3D2E0A")
    private val cBad = Color.parseColor("#4A1212")
    private val cTimerBg = Color.parseColor("#0F1F2E")

    private lateinit var speedTv: TextView
    private lateinit var timeTv: TextView
    private lateinit var statusTv: TextView
    private lateinit var bar: ProgressBar
    private lateinit var btn: Button
    private lateinit var limitEt: EditText
    private lateinit var durEt: EditText
    private lateinit var testCb: CheckBox
    private lateinit var testBox: LinearLayout
    private lateinit var testSb: SeekBar
    private lateinit var testTv: TextView
    private lateinit var gpsStatusTv: TextView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

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
        if (stroke != 0) g.setStroke(dp(1), stroke)
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
        b.textSize = 17f
        b.setTypeface(b.typeface, Typeface.BOLD)
        b.setTextColor(Color.WHITE)
        b.background = shape(color, dp(30))
        b.minHeight = dp(54)
        b.minimumHeight = dp(54)
        b.stateListAnimator = null
        return b
    }

    private fun paint(b: Button, color: Int) {
        b.background = shape(color, dp(30))
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(18), dp(18), dp(18), dp(18))
        c.background = shape(cPanel, dp(20), cLine)
        return c
    }

    private fun numberField(value: Int): EditText {
        val e = EditText(this)
        e.inputType = InputType.TYPE_CLASS_NUMBER
        e.setText(value.toString())
        e.textSize = 17f
        e.setTypeface(e.typeface, Typeface.BOLD)
        e.setTextColor(cText)
        e.gravity = Gravity.CENTER
        e.background = shape(Color.parseColor("#0D1117"), dp(12), cLine)
        e.setPadding(dp(12), dp(10), dp(12), dp(10))
        e.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveSettings()
        }
        return e
    }

    private fun settingRow(title: String, field: EditText): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(
            label(title, 15f),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        r.addView(field, LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT))
        return r
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("ayar", MODE_PRIVATE)
        val limit = limitEt.text.toString().toIntOrNull() ?: 90
        val dur = durEt.text.toString().toIntOrNull() ?: 45
        prefs.edit()
            .putInt("limit", limit.coerceIn(10, 250))
            .putInt("dur", dur.coerceIn(5, 600))
            .apply()
        SpeedService.speedLimit = limit.coerceIn(10, 250)
        SpeedService.alertDuration = dur.coerceIn(5, 600)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeedService.testMode = false
        val prefs = getSharedPreferences("ayar", MODE_PRIVATE)

        window.statusBarColor = cBg
        window.navigationBarColor = cBg

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(cBg)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(28), dp(16), dp(40))

        // Başlık
        root.addView(label("Hız aşımı sayacı", 22f, cBlueText, true), lp())
        root.addView(
            label(
                "Hız sınırı üstünde kesintisiz geçen süreyi sayar, süre dolunca sesli uyarı verir.",
                14f, cMuted
            ),
            lp(dp(6))
        )

        // Ana kart (hız + timer + buton)
        val mainCard = card()

        val l1 = label("Anlık GPS hızı", 14f, cMuted)
        l1.gravity = Gravity.CENTER
        mainCard.addView(l1, lp())

        speedTv = label("--", 72f, cText, true)
        speedTv.gravity = Gravity.CENTER
        speedTv.setSingleLine(true)
        mainCard.addView(speedTv, lp())

        val unit = label("km/sa", 18f, cMuted)
        unit.gravity = Gravity.CENTER
        mainCard.addView(unit, lp(dp(2)))

        // Timer kutusu
        val timerBox = LinearLayout(this)
        timerBox.orientation = LinearLayout.VERTICAL
        timerBox.setPadding(dp(16), dp(18), dp(16), dp(16))
        timerBg.cornerRadius = dp(16).toFloat()
        timerBg.setColor(cTimerBg)
        timerBg.setStroke(dp(1), Color.parseColor("#1A2E42"))
        timerBox.background = timerBg

        timeTv = label("00:00 / 00:45", 40f, Color.WHITE, true)
        timeTv.gravity = Gravity.CENTER
        timeTv.setSingleLine(true)
        timerBox.addView(timeTv, lp())

        statusTv = label("Hız sınırın altında", 15f, Color.parseColor("#A0AEC0"))
        statusTv.gravity = Gravity.CENTER
        timerBox.addView(statusTv, lp(dp(6)))

        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        bar.max = 1000
        bar.progressTintList = ColorStateList.valueOf(Color.WHITE)
        bar.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#26FFFFFF"))
        val barLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6))
        barLp.topMargin = dp(14)
        timerBox.addView(bar, barLp)

        mainCard.addView(timerBox, lp(dp(16)))

        // Yeşil buton
        btn = pill("Takibi başlat", cOk)
        btn.setOnClickListener { onToggle() }
        mainCard.addView(btn, lp(dp(16)))

        // GPS durum yazısı
        gpsStatusTv = label("Başlamak için düğmeye bas ve konum iznini ver.", 13f, cMuted)
        gpsStatusTv.gravity = Gravity.CENTER
        mainCard.addView(gpsStatusTv, lp(dp(10)))

        root.addView(mainCard, lp(dp(16)))

        // Ayarlar kartı
        val setCard = card()
        limitEt = numberField(prefs.getInt("limit", 90))
        durEt = numberField(prefs.getInt("dur", 45))
        setCard.addView(settingRow("Hız sınırı (km/sa)", limitEt), lp())
        setCard.addView(settingRow("Uyarı süresi (saniye)", durEt), lp(dp(14)))
        root.addView(setCard, lp(dp(12)))

        // Test modu
        val testCard = card()
        testCb = CheckBox(this)
        testCb.text = "Test modu (GPS yerine kaydırıcı)"
        testCb.setTextColor(cText)
        testCb.setOnCheckedChangeListener { _, checked ->
            SpeedService.testMode = checked
            testBox.visibility = if (checked) View.VISIBLE else View.GONE
            if (SpeedService.running) {
                // Yeniden başlat
                stopTracking()
                startTracking()
            }
        }
        testCard.addView(testCb, lp())

        testBox = LinearLayout(this)
        testBox.orientation = LinearLayout.VERTICAL
        testBox.visibility = View.GONE

        testTv = label("Sahte hız: 95 km/sa", 14f, cMuted)
        testBox.addView(testTv, lp(dp(8)))

        testSb = SeekBar(this)
        testSb.max = 160
        testSb.progress = 95
        testSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                testTv.text = "Sahte hız: $progress km/sa"
                if (SpeedService.testMode) {
                    SpeedService.fakeSpeed = progress.toFloat()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        testBox.addView(testSb, lp(dp(4)))
        testCard.addView(testBox, lp())

        root.addView(testCard, lp(dp(12)))

        // Not
        val note = label(
            "Sınırın üstündeki hız algılanınca sayaç başlar. Hız sınıra eşit ya da altına düşerse sayaç sıfırlanır. Süre kesintisiz dolarsa yüksek sesli alarm ve titreşim devreye girer.\n\nTakip sırasında ekran açık tutulur.",
            13f, cMuted
        )
        root.addView(note, lp(dp(16)))

        scroll.addView(root)
        setContentView(scroll)

        // Servis değerlerini yükle
        SpeedService.speedLimit = prefs.getInt("limit", 90)
        SpeedService.alertDuration = prefs.getInt("dur", 45)

        // UI güncelleme döngüsü
        startUiLoop()
    }

    private fun startUiLoop() {
        handler.post(object : Runnable {
            override fun run() {
                updateUi()
                handler.postDelayed(this, 250)
            }
        })
    }

    private fun updateUi() {
        val speed = if (SpeedService.testMode) SpeedService.fakeSpeed else SpeedService.currentSpeed
        speedTv.text = if (speed < 0) "--" else speed.toInt().toString()

        val limit = limitEt.text.toString().toIntOrNull() ?: 90
        val total = durEt.text.toString().toIntOrNull() ?: 45
        val elapsed = SpeedService.overspeedSeconds

        timeTv.text = String.format("%02d:%02d / %02d:%02d",
            elapsed / 60, elapsed % 60, total / 60, total % 60)

        bar.progress = if (total > 0) ((elapsed.toFloat() / total) * 1000).toInt().coerceIn(0, 1000) else 0

        if (!SpeedService.running) {
            statusTv.text = "Hız sınırın altında"
            statusTv.setTextColor(Color.parseColor("#A0AEC0"))
            timerBg.setColor(cTimerBg)
            gpsStatusTv.text = "Takip durduruldu."
            paint(btn, cOk)
            btn.text = "Takibi başlat"
        } else {
            paint(btn, cStop)
            btn.text = "Takibi durdur"

            if (speed > limit) {
                if (elapsed >= total) {
                    statusTv.text = "Hız sınırı aşıldı! Yavaşla."
                    statusTv.setTextColor(Color.WHITE)
                    timerBg.setColor(cBad)
                } else {
                    statusTv.text = "${total - elapsed} saniye sonra sesli uyarı"
                    statusTv.setTextColor(Color.WHITE)
                    timerBg.setColor(cWarn)
                }
            } else {
                statusTv.text = "Hız sınırın altında"
                statusTv.setTextColor(Color.parseColor("#A0AEC0"))
                timerBg.setColor(cTimerBg)
            }

            if (SpeedService.testMode) {
                gpsStatusTv.text = "Test modu: sahte hız kullanılıyor."
            } else {
                gpsStatusTv.text = SpeedService.gpsStatus
            }
        }
    }

    private fun onToggle() {
        if (SpeedService.running) {
            stopTracking()
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        saveSettings()

        if (!SpeedService.testMode) {
            if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                return
            }
        }

        SpeedService.running = true
        SpeedService.overspeedSeconds = 0
        SpeedService.fakeSpeed = testSb.progress.toFloat()

        val intent = Intent(this, SpeedService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateUi()
    }

    private fun stopTracking() {
        SpeedService.running = false
        stopService(Intent(this, SpeedService::class.java))
        updateUi()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startTracking()
        } else {
            Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
