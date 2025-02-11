package com.example.bai1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Android 13+ (API 33): Xin quyền READ_MEDIA_AUDIO thay vì READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6+ yêu cầu quyền runtime
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Android 13+ (API 33): Xin quyền POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Nếu có quyền cần xin, yêu cầu người dùng cấp quyền
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        } else {
            scanFiles() // Nếu đã có quyền, thực hiện hành động ngay
        }
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

    // Xử lý kết quả cấp quyền
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val allPermissionsGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allPermissionsGranted) {
                scanFiles()
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền để truy cập file âm thanh!", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
