package com.example.bai1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bai1.databinding.ActivityDetailBinding
import com.example.bai1.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 123 // Mã yêu cầu quyền
    private val REQUIRED_PERMISSION = Manifest.permission.READ_MEDIA_AUDIO // Quyền cần xin
    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private val fileList = mutableListOf<FileItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = binding.rvMain
        fileAdapter = FileAdapter(fileList)
        recyclerView.adapter = fileAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Kiểm tra quyền khi ứng dụng được mở
        if (ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            // Nếu quyền chưa được cấp, yêu cầu quyền
            requestPermission()
        } else {
            scanFiles()
        }
    }

    private fun requestPermission() {
        // Yêu cầu quyền
        ActivityCompat.requestPermissions(this, arrayOf(REQUIRED_PERMISSION), REQUEST_CODE_PERMISSIONS)
    }

    // Hàm quét file âm thanh
    private fun scanFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Chỉ định các cột bạn muốn truy vấn từ MediaStore
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE
            )

            val sortOrder =
                "${MediaStore.MediaColumns.DATE_ADDED} DESC" // Sắp xếp theo thời gian thêm vào


            // Quét âm thanh
            val cursorAudio = applicationContext.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, // Quét tất cả âm thanh
                null, // Không cần điều kiện
                sortOrder
            )

            val newFileItems = mutableListOf<FileItem>()


            cursorAudio?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)) / (1024 * 1024) // MB
                    val type = "AUDIO"
                    val fileItem = FileItem(name, "content://media/external/audio/media/$id", size, type)
                    newFileItems.add(fileItem)
                }
            }

            // Cập nhật UI (RecyclerView) trên thread chính
            withContext(Dispatchers.Main) {
                fileAdapter.addNewFiles(newFileItems) // Thêm các file quét được vào đầu danh sách
            }
        }
    }

    // Xử lý kết quả yêu cầu quyền
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền đã được cấp, bạn có thể quét file âm thanh
                scanFiles()
            }
        }
    }
}
