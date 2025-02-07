package com.example.bai1

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(private val fileList: MutableList<FileItem>) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.tv_fileName)
        val fileIcon: ImageView = view.findViewById(R.id.iv_fileIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = fileList[position]
        holder.fileName.text = file.name

        // Tạo biểu tượng tùy theo kiểu file (dùng chuỗi thay vì enum)
        when (file.type) {
            "AUDIO" -> holder.fileIcon.setImageResource(R.drawable.image_music)
        }

        // Chuyển sang bên Detail
        holder.itemView.setOnClickListener {
            val intent = Intent(it.context, DetailActivity::class.java)
            intent.putExtra("fileItemPath", file.path) // Truyền đường dẫn audio
            intent.putExtra("fileName", file.name) // Truyền đường dẫn audio
            // truyền danh sách các bài hát sang bên DetailActivity
            intent.putExtra("fileList", ArrayList(fileList)) // Truyền danh sách fileList qua Intent
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = fileList.size

    // Hàm để thêm file mới lên đầu danh sách
    fun addNewFiles(files: List<FileItem>) {
        fileList.addAll(0, files)
        notifyItemRangeInserted(0, files.size)
    }
}