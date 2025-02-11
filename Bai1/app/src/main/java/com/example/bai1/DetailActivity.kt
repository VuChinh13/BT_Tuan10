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
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import com.example.bai1.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    private var fileList = mutableListOf<FileItem>()
    private lateinit var timeUpdateReceiver: BroadcastReceiver  // Khai báo biến receiver toàn cục
    private lateinit var musicUpdateReceiver: BroadcastReceiver  // Khai báo biến receiver toàn cục

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


        musicUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val status = intent?.getStringExtra("STATUS").toString()
                Log.d("chinh", status)
                if (status == "PAUSE") {
                    binding.ivPause.visibility = View.GONE
                    binding.ivPlay.visibility = View.VISIBLE
                    pauseMusic()
                } else if (status == "NEXT") {
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
                } else if (status == "PREVIOUS") {
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
                } else if (status == "PLAY"){
                    binding.ivPlay.visibility = View.GONE
                    binding.ivPause.visibility = View.VISIBLE
                    playMusic(currentIndex)  // Tiếp tục phát nhạc
                }
            }
        }

        val filter1 = IntentFilter("com.example.bai1.MUSIC_UPDATE")
        registerReceiver(musicUpdateReceiver, filter1, RECEIVER_NOT_EXPORTED)


        // Khi ấn nút Play
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
            currentIndex = if (currentIndex > 0) {
                currentIndex - 1 // Lùi về bài trước
            } else {
                fileList.size - 1 // Nếu đang ở bài đầu, quay lại bài cuối
            }
            binding.ivPause.visibility = View.VISIBLE
            binding.ivPlay.visibility = View.GONE
            // hiển thị cái tên bài hát
            binding.tvTen.text = fileList.get(currentIndex).name
            stopMusic()
            playMusic(currentIndex)  // Phát bài mới
        }
    }

    // Phương thức play nhạc
    @OptIn(UnstableApi::class)
    private fun playMusic(index: Int) {
        val serviceIntent = Intent(this, MusicService::class.java)
        val audioUri = fileList[index].path  // Lấy đường dẫn của bài hát ở chỉ số index
        serviceIntent.putExtra("audioUri", audioUri)  // Truyền đường dẫn audio vào MusicService
        serviceIntent.putExtra(
            "songTitle",
            fileList[index].name
        )  // Truyền đường dẫn audio vào MusicService
        serviceIntent.putExtra(
            "fileList",
            ArrayList(fileList)
        ) // Truyền danh sách fileList qua Intent
        serviceIntent.putExtra(
            "currentIndex",
            currentIndex
        ) // Đoạn code này truyền bài hát hiện tại mà đang phát
        serviceIntent.action = "PLAY" // Đảm bảo phát nhạc khi bắt đầu
        startService(serviceIntent)
    }

    // Phương thức pause nhạc
    @OptIn(UnstableApi::class)
    private fun pauseMusic() {
        val serviceIntent = Intent(this, MusicService::class.java)
        val audioUri = fileList[currentIndex].path  // Lấy đường dẫn của bài hát ở chỉ số index
        serviceIntent.putExtra("audioUri", audioUri)  // Truyền đường dẫn audio vào MusicService
        serviceIntent.putExtra(
            "songTitle",
            fileList[currentIndex].name
        )  // Truyền đường dẫn audio vào MusicService
        serviceIntent.putExtra(
            "fileList",
            ArrayList(fileList)
        ) // Truyền danh sách fileList qua Intent
        serviceIntent.putExtra(
            "currentIndex",
            currentIndex
        ) // Đoạn code này truyền bài hát hiện tại mà đang phát
        serviceIntent.action = "PAUSE"
        startService(serviceIntent)
    }

    // Dừng nhạc
    @OptIn(UnstableApi::class)
    private fun stopMusic() {
        val serviceIntent = Intent(this, MusicService::class.java)
        serviceIntent.putExtra(
            "songTitle",
            fileList[currentIndex].name
        )  // Truyền đường dẫn audio vào MusicService
        serviceIntent.putExtra(
            "fileList",
            ArrayList(fileList)
        ) // Truyền danh sách fileList qua Intent
        serviceIntent.action = "STOP"
        startService(serviceIntent)
    }

    // Hàm format thời gian (chuyển mili giây thành phút:giây)
    private fun formatTime(timeInMillis: Int): String {
        val minutes = timeInMillis / 60000
        val seconds = (timeInMillis % 60000) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        super.onDestroy()
        // Dừng nhạc và dừng service khi Activity bị hủy
        val serviceIntent = Intent(this, MusicService::class.java)
        stopService(serviceIntent)
        unregisterReceiver(timeUpdateReceiver)  // Hủy đăng ký receiver
        unregisterReceiver(musicUpdateReceiver)
    }
}






