package com.example.bai1

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.FileInputStream

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val CHANNEL_ID = "music_service_channel"
    private val NOTIFICATION_ID = 1
    private val timeUpdateInterval = 1000L // Cập nhật thời gian mỗi giây
    private val handler = Handler()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        when (action) {
            "PLAY" -> playMusic(intent)  // Truyền intent vào playMusic()
            "PAUSE" -> pauseMusic()
            "STOP" -> stopMusic()
        }
        return START_STICKY
    }


    private fun playMusic(intent: Intent) {
        if (mediaPlayer == null) {
            val audioUri = intent.getStringExtra("audioUri")
            if (!audioUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(audioUri)
                    val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                    val fileInputStream = FileInputStream(fileDescriptor?.fileDescriptor)
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(fileInputStream.fd)
                        prepare()
                        start()
                    }
                    // Gửi thông tin thời gian qua BroadcastReceiver
                    handler.postDelayed(timeUpdateRunnable, timeUpdateInterval)
                } catch (e: Exception) {
                    Log.e("MusicService", "Error setting data source: ${e.message}")
                }
            }
        } else {
            mediaPlayer?.start()  // Resume playing if it's already initialized
        }
    }


    private fun pauseMusic() {
        mediaPlayer?.pause()
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacks(timeUpdateRunnable) // Dừng cập nhật thời gian khi service bị hủy
    }

    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            val intent = Intent("com.example.bai1.TIME_UPDATE")
            intent.putExtra("currentPosition", mediaPlayer?.currentPosition ?: 0)
            intent.putExtra("duration", mediaPlayer?.duration ?: 0)
            sendBroadcast(intent)
            handler.postDelayed(this, timeUpdateInterval) // Tiếp tục cập nhật mỗi giây
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}



