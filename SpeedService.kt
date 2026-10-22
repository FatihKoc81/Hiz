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
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator

class SpeedService : Service() {

    companion object {
        @Volatile var running = false
        @Volatile var speedKmh = 0f
        @Volatile var overSec = 0
        @Volatile var alarming = false
        @Volatile var gpsOk = false
        @Volatile var testMode = false
        @Volatile var testSpeed = 0f
        @Volatile var listener: (() -> Unit)? = null
        private const val CH = "takip"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lm: LocationManager? = null
    private var wl: PowerManager.WakeLock? = null
    private var player: MediaPlayer? = null
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
            val sp = if (testMode) testSpeed else if (gpsOk) gpsSpeed else 0f
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

    private fun startAlarm() {
        alarming = true
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mp = MediaPlayer()
            mp.setAudioAttributes(attrs)
            mp.setDataSource(this, uri)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            player = mp
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
            player?.stop()
            player?.release()
        } catch (e: Exception) {
        }
        player = null
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
