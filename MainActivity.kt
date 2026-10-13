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

    private val cBg = Color.parseColor("#0B1424")
    private val cPanel = Color.parseColor("#13213A")
    private val cLine = Color.parseColor("#25406B")
    private val cText = Color.parseColor("#F1F5FB")
    private val cMuted = Color.parseColor("#9FB0C8")
    private val cBlueText = Color.parseColor("#8AB8FF")
    private val cBlue = Color.parseColor("#2F6FE4")
    private val cStop = Color.parseColor("#3A4A66")
    private val cOk = Color.parseColor("#1F9D63")
    private val cWarn = Color.parseColor("#7A5A10")
    private val cBad = Color.parseColor("#C0392B")
    private val cRun = Color.parseColor("#123B2C")

    private lateinit var speedTv: TextView
    private lateinit var timeTv: TextView
    private lateinit var statusTv: TextView
    private lateinit var bar: ProgressBar
    private lateinit var btn: Button
    private lateinit var saveBtn: Button
    private lateinit var limitEt: EditText
    private lateinit var durEt: EditText
    private lateinit var testCb: CheckBox
    private lateinit var testBox: LinearLayout
    private lateinit var testSb: SeekBar
    private lateinit var testTv: TextView

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
        c.setPadding(dp(16), dp(16), dp(16), dp(16))
        c.background = shape(cPanel, dp(20), cLine)
        return c
    }

    private fun numberField(value: Int): EditText {
        val e = EditText(this)
        e.inputType = InputType.TYPE_CLASS_NUMBER
        e.setText(value.toString())
        e.textSize = 18f
        e.setTextColor(cText)
        e.gravity = Gravity.CENTER
        e.background = shape(cBg, dp(12), cLine)
        e.setPadding(dp(10), dp(10), dp(10), dp(10))
        return e
    }

    private fun settingRow(title: String, field: EditText): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(
            label(title, 16f),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        r.addView(field, LinearLayout.LayoutParams(dp(96), ViewGroup.LayoutParams.WRAP_CONTENT))
        return r
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeedService.testMode = false
        val prefs = getSharedPreferences("ayar", MODE_PRIVATE)

        window.statusBarColor = cBg
        window.navigationBarColor = cBg

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(18), dp(36), dp(18), dp(40))

        root.addView(label("Hız Aşımı Sayacı", 24f, cBlueText, true), lp())
        root.addView(
            label(
                "Hız sınırı üstünde kesintisiz geçen süreyi sayar, süre dolunca sesli uyarı verir.",
                14f, cMuted
            ),
            lp(dp(4))
        )

        val speedCard = card()
        val l1 = label("Anlık GPS hızı", 15f, cMuted)
        l1.gravity = Gravity.CENTER
        speedCard.addView(l1, lp())
        speedTv = label("--", 96f, cText, true)
        speedTv.gravity = Gravity.CENTER
        speedTv.setSingleLine(true)
        speedCard.addView(speedTv, lp())
        val unit = label("km/sa", 22f, cBlueText)
        unit.gravity = Gravity.CENTER
        speedCard.addView(unit, lp())
        root.addView(speedCard, lp(dp(18)))

        val timerBox = LinearLayout(this)
        timerBox.orientation = LinearLayout.VERTICAL
        timerBox.setPadding(dp(16), dp(20), dp(16), dp(18))
        timerBg.cornerRadius = dp(18).toFloat()
        timerBg.setColor(cPanel)
        timerBox.background = timerBg
        timeTv = label("00:00 / 00:45", 34f, Color.WHITE, true)
        timeTv.gravity = Gravity.CENTER
        timeTv.setSingleLine(true)
        statusTv = label("Takip kapalı", 16f, Color.WHITE)
        statusTv.gravity = Gravity.CENTER
        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        bar.max = 1000
        bar.progressTintList = ColorStateList.valueOf(Color.WHITE)
        bar.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#44FFFFFF"))
        timerBox.addView(timeTv, lp())
        timerBox.addView(statusTv, lp(dp(6)))
        val barLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8))
        barLp.topMargin = dp(14)
        timerBox.addView(bar, barLp)
        root.addView(timerBox, lp(dp(14)))

        btn = pill("Takibi başlat", cBlue)
        btn.setOnClickListener { onToggle() }
        root.addView(btn, lp(dp(16)))

        saveBtn = pill("Kaydet", cBlue)
        saveBtn.setOnClickListener { onSave() }
        root.addView(saveBtn, lp(dp(10)))

        val setCard = card()
        limitEt = numberField(prefs.getInt("limit", 90))
        durEt = numberField(prefs.getInt("dur", 45))
        setCard.addView(settingRow("Hız sınırı (km/sa)", limitEt), lp())
        setCard.addView(settingRow("Uyarı süresi (sn)", durEt), lp(dp(14)))
        root.addView(setCard, lp(dp(18)))

        testCb = CheckBox(this)
        testCb.text = "Test modu (GPS yerine kaydırıcı)"
        testCb.setTextColor(cBlueText)
        testCb.setOnCheckedChangeListener { _, c ->
            SpeedService.testMode = c
            test
