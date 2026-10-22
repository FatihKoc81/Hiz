package com.hizuyari

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class SpeedService : Service() {

    companion object {
        @Volatile var running = false
        @Volatile var speedKmh = 0f
        @Volatile var overSec = 0
        @Volatile var alarming = false
        @Volatile var gpsOk = false
        @Volatile var listener: (() -> Unit)? = null
        private const val CH = "takip"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lm: LocationManager? = null
    private var wl: PowerManager.WakeLock? = null
    private var siren: AudioTrack? = null
    private var gpsSpeed = 0f
    private var lastFix = 0L
    private var overSince = -1L

    private val gps = object : LocationListener {
        override fun onLocationChanged(l: Location) {
            lastFix = SystemClock.elapsedRealtime()
            gpsSpeed = if (l.hasSpeed()) l.speed * 3.6f else 0f
        }

        @Deprecated("eski API")
        override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
        override fun onProviderEnabled(p: String) {}
        override fun onProviderDisabled(p: String) {}
    }

    private val tick = object : Runnable {
        override fun run() {
            val pr = getSharedPreferences("ayar", MODE_PRIVATE)
            val limit = pr.getInt("limit", 90)
            val dur = pr.getInt("dur", 45)
            val now = SystemClock.elapsedRealtime()

            gpsOk = now - lastFix < 5000
            val sp = if (gpsOk) gpsSpeed else 0f
            speedKmh = sp

            if (sp > limit) {
                if (overSince < 0) overSince = now
                overSec = ((now - overSince) / 1000).toInt()
            } else {
                overSince = -1L
                overSec = 0
            }

            val shouldAlarm = sp > limit && overSec >= dur
            if (shouldAlarm && !alarming) startAlarm()
            else if (!shouldAlarm && alarming) stopAlarm()

            listener?.invoke()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_STICKY
        try {
            startFg()
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        running = true

        val pm = getSystemService(PowerManager::class.java)
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hizuyari:takip")
        wl?.acquire(12 * 60 * 60 * 1000L)

        try {
            lm = getSystemService(LocationManager::class.java)
            lm?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0f, gps, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
            return START_NOT_STICKY
        }

        handler.post(tick)
        return START_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CH, "Hız takibi", NotificationManager.IMPORTANCE_MIN)
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)

        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Hız takibi açık")
            .setOngoing(true)
            .setContentIntent(pi)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, n)
        }
    }

    // Savaş sireni: 450-900 Hz arası yavaşça yükselip alçalan, kesintisiz döngüde çalan ses.
    private fun buildSiren(attrs: AudioAttributes): AudioTrack {
        val rate = 22050
        val n = rate * 6
        val buf = ShortArray(n)

        fun freq(i: Int): Double = 450.0 + 450.0 * (0.5 - 0.5 * cos(2.0 * PI * i / n))

        var cycles = 0.0
        for (i in 0 until n) cycles += freq(i) / rate
        val scale = cycles.roundToInt() / cycles

        var phase = 0.0
        for (i in 0 until n) {
            phase += 2.0 * PI * freq(i) * scale / rate
            val v = 0.62 * sin(phase) + 0.25 * sin(2 * phase) + 0.13 * sin(3 * phase)
            buf[i] = (v * 30000).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(n * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buf, 0, n)
        track.setLoopPoints(0, n, -1)
        return track
    }

    private fun startAlarm() {
        alarming = true
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            val t = buildSiren(attrs)
            t.play()
            siren = t
        } catch (e: Exception) {
        }
        try {
            val v = getSystemService(Vibrator::class.java)
            @Suppress("DEPRECATION")
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 400), 0), attrs)
        } catch (e: Exception) {
        }
    }

    private fun stopAlarm() {
        alarming = false
        try {
            siren?.stop()
            siren?.release()
        } catch (e: Exception) {
        }
        siren = null
        try {
            getSystemService(Vibrator::class.java).cancel()
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        stopAlarm()
        try {
            lm?.removeUpdates(gps)
        } catch (e: Exception) {
        }
        try {
            if (wl?.isHeld == true) wl?.release()
        } catch (e: Exception) {
        }
        running = false
        speedKmh = 0f
        overSec = 0
        gpsOk = false
        listener?.invoke()
        super.onDestroy()
    }
}
