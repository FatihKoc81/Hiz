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

    private val cBg = Color.parseColor("#0B0E14")
    private val cPanel = Color.parseColor("#12161F")
    private val cLine = Color.parseColor("#1E2430")
    private val cText = Color.parseColor("#E8EAED")
    private val cMuted = Color.parseColor("#8B93A0")
    private val cBlueText = Color.parseColor("#60A5FA")
    private val cOk = Color.parseColor("#22C55E")
    private val cStop = Color.parseColor("#3A3F45")
    private val cWarn = Color.parseColor("#3D2E0A")
    private val cBad = Color.parseColor("#4A1212")
    private val cTimerBg = Color.parseColor("#0F1F2E")

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
        root.orientation = Linear
