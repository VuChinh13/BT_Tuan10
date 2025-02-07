package com.example.bai1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bai1.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    private var fileList = mutableListOf<FileItem>()
    private lateinit var timeUpdateReceiver: BroadcastReceiver  // Khai báo biến receiver toàn cục

    // Biến dùng để lưu cái bài hát hiện tại mà đang phát
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy audioUri từ Intent
        val audioUri = intent.getStringExtra("fileItemPath")
        val fileName = intent.getStringExtra("fileName")
        // Nhận fileList từ Intent
        fileList = intent.getSerializableExtra("fileList") as ArrayList<FileItem>

        // Hiển thị tên bài hát
        binding.tvTen.text = fileName

        // Phải lấy ra được chỉ số của bài hát
        currentIndex = fileList.indexOfFirst { it.path == audioUri }
        playMusic(currentIndex)

        // Nhận cập nhật thời gian từ MusicService
        timeUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val currentPosition = intent?.getIntExtra("currentPosition", 0) ?: 0
                val duration = intent?.getIntExtra("duration", 0) ?: 0

                binding.tvTimeStart.text = formatTime(currentPosition)
                binding.tvTimeTotal.text = formatTime(duration)
                binding.sbMusic.max = duration
                binding.sbMusic.progress = currentPosition
            }
        }

        val filter = IntentFilter("com.example.bai1.TIME_UPDATE")
        registerReceiver(timeUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // Khi ấn nút Play/Pause
        binding.ivPlay.setOnClickListener {
            binding.ivPlay.visibility = View.GONE
            binding.ivPause.visibility = View.VISIBLE
            playMusic(currentIndex)  // Tiếp tục phát nhạc
        }

        // Khi ấn nút Pause
        binding.ivPause.setOnClickListener {
            binding.ivPause.visibility = View.GONE
            binding.ivPlay.visibility = View.VISIBLE
            pauseMusic()  // Tạm dừng nhạc
        }

        // Khi ấn nút Next (Tiến bài)
        binding.ivSkipForward.setOnClickListener {
            stopMusic()
            currentIndex = if (currentIndex < fileList.size - 1) {
                currentIndex + 1 // Tiến tới bài sau
            } else {
                0 // Nếu đang ở bài cuối, quay lại bài đầu tiên
            }
            // Đổi tên bài hát và chuyển đổi cái nút thành nút pause
            binding.ivPause.visibility = View.VISIBLE
            binding.ivPlay.visibility = View.GONE
            // hiển thị cái tên bài hát
            binding.tvTen.text = fileList.get(currentIndex).name
            playMusic(currentIndex)  // Phát bài mới
        }

        // Khi ấn nút Previous (Lùi bài)
        binding.ivSkipBack.setOnClickListener {
            stopMusic()
            currentIndex = if (currentIndex > 0) {
                currentIndex - 1 // Lùi về bài trước
            } else {
                fileList.size - 1 // Nếu đang ở bài đầu, quay lại bài cuối
            }
            binding.ivPause.visibility = View.VISIBLE
            binding.ivPlay.visibility = View.GONE
            // hiển thị cái tên bài hát
            binding.tvTen.text = fileList.get(currentIndex).name
            playMusic(currentIndex)  // Phát bài mới
        }
    }

    // Phương thức play nhạc
    private fun playMusic(index: Int) {
        val serviceIntent = Intent(this, MusicService::class.java)
        val audioUri = fileList[index].path  // Lấy đường dẫn của bài hát ở chỉ số index
        serviceIntent.putExtra("audioUri", audioUri)  // Truyền đường dẫn audio vào MusicService
        serviceIntent.action = "PLAY" // Đảm bảo phát nhạc khi bắt đầu
        startService(serviceIntent)
    }

    // Phương thức pause nhạc
    private fun pauseMusic() {
        val serviceIntent = Intent(this, MusicService::class.java)
        serviceIntent.action = "PAUSE"
        startService(serviceIntent)
    }

    // Dừng nhạc
    private fun stopMusic() {
        val serviceIntent = Intent(this, MusicService::class.java)
        serviceIntent.action = "STOP"
        startService(serviceIntent)
    }

    // Hàm format thời gian (chuyển mili giây thành phút:giây)
    private fun formatTime(timeInMillis: Int): String {
        val minutes = timeInMillis / 60000
        val seconds = (timeInMillis % 60000) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dừng nhạc và dừng service khi Activity bị hủy
        stopMusic()
        unregisterReceiver(timeUpdateReceiver)  // Hủy đăng ký receiver
    }
}





