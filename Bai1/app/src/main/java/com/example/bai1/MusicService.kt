package com.example.bai1

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaStyleNotificationHelper
import java.io.FileInputStream
import android.support.v4.media.session.MediaSessionCompat
import android.view.View


@UnstableApi
class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val CHANNEL_ID = "music_service_channel"
    private val NOTIFICATION_ID = 1
    private val timeUpdateInterval = 1000L // Cập nhật thời gian mỗi giây
    private val handler = Handler()
    private var songTitle = ""
    private var fileList = mutableListOf<FileItem>() // chứa danh sách bài hát
    private var currentIndex = 0
    private var isPlaying = false
    private lateinit var mediaSession: MediaSessionCompat
    // Khai báo PendingIntent cho các nút điều khiển

    private val prevPendingIntent: PendingIntent by lazy {
        val intent = Intent("com.example.bai1.MUSIC_UPDATE").apply {
            putExtra("STATUS", "PREVIOUS") // Gửi trạng thái PAUSE
        }
        PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    private val pausePendingIntent: PendingIntent by lazy {
        if (isPlaying) { // nếu mà nhạc đang chạy
            isPlaying = false
            val intent = Intent("com.example.bai1.MUSIC_UPDATE").apply {
                putExtra("STATUS", "PAUSE") // Gửi trạng thái
            }
            PendingIntent.getBroadcast(
                this,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }else{ // nếu mà đang bị tạm dừng thì chuyển icon
            val intent = Intent("com.example.bai1.MUSIC_UPDATE").apply {
                putExtra("STATUS", "PLAY") // Gửi trạng thái
            }
            PendingIntent.getBroadcast(
                this,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private val nextPendingIntent: PendingIntent by lazy {
        val intent = Intent("com.example.bai1.MUSIC_UPDATE").apply {
            putExtra("STATUS", "NEXT") // Gửi trạng thái PAUSE
        }
        PendingIntent.getBroadcast(
            this,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate(){
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            isActive = true // Kích hoạt media session
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        // Nhận tên bài hát và danh sách bài hát
        songTitle = intent.getStringExtra("songTitle").toString()
        // Nhận fileList từ Intent
        fileList = intent.getSerializableExtra("fileList") as ArrayList<FileItem>
        Log.d("Kiểm tra", fileList[0].name)
        // Lấy ra chỉ số bài hát mà đang phát
        currentIndex = intent.getIntExtra("currentIndex",0)
        // Lấy đường dẫn
        val audioUri = intent.getStringExtra("audioUri").toString()
        when (action) {
            "PLAY" -> playMusic(audioUri)  // Truyền intent vào playMusic()
            "PAUSE" -> pauseMusic()
            "STOP" -> stopMusic()
        }
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }


    private fun playMusic(audioUri: String) {
        if (mediaPlayer == null) {
            if (audioUri.isNotEmpty()) {
                try {
                    val uri = Uri.parse(audioUri)
                    val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                    val fileInputStream = FileInputStream(fileDescriptor?.fileDescriptor)
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(fileInputStream.fd)
                        prepare()
                        start()
                    }
                    isPlaying = true // Đang phát nhạc
                    createNotification(R.drawable.ic_pause_2) // Hiển thị thông báo khi mà chơi nhạc
                    // Gửi thông tin thời gian qua BroadcastReceiver
                    handler.postDelayed(timeUpdateRunnable, timeUpdateInterval)
                } catch (e: Exception) {
                    Log.e("MusicService", "Error setting data source: ${e.message}")
                }
            }
        } else {
            isPlaying = true// Đang phát nhạc
            mediaPlayer?.start()
           createNotification(R.drawable.ic_pause_2) // Hiển thị thông báo khi mà chơi nhạc
        }
    }


    private fun pauseMusic() {
        isPlaying = false // Đã tạm dừng nhạc
        mediaPlayer?.pause()
        createNotification(R.drawable.ic_play) // Hiển thị thông báo
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



    @OptIn(UnstableApi::class)
    private fun createNotification(playPauseIcon:Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.image_music)
            .addAction(R.drawable.skip_back_2, "Previous", prevPendingIntent) // Nút "Previous"
            .addAction(playPauseIcon, "Pause", pausePendingIntent) // Nút "Pause" hoặc là Play
            .addAction(R.drawable.skip_forward_2, "Next", nextPendingIntent) // Nút "Next"
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken) // Gắn media session
                    .setShowActionsInCompactView(0, 1, 2) // Hiển thị cả 3 nút: Previous, Pause, Next
            )
            .setContentTitle(fileList[currentIndex].name)
            .setContentText("Bài hát của tôi")
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.image_music_main))
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }




    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}






