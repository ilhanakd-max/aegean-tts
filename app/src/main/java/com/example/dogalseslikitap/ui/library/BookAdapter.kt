package com.example.dogalseslikitap.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dogalseslikitap.data.db.BookEntity
import com.example.dogalseslikitap.databinding.ItemBookBinding
import com.example.dogalseslikitap.model.BookType

class BookAdapter(private val onClick: (BookEntity) -> Unit) :
    RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private val items = mutableListOf<BookEntity>()

    fun submitList(data: List<BookEntity>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: BookEntity) {
            binding.txtTitle.text = book.title
            binding.txtPath.text = book.path
            val iconRes = when (BookType.fromPath(book.path)) {
                BookType.EPUB -> android.R.drawable.ic_menu_recent_history
                BookType.PDF -> android.R.drawable.ic_menu_agenda
                BookType.TXT -> android.R.drawable.ic_menu_edit
            }
            binding.imgType.setImageResource(iconRes)
            binding.root.setOnClickListener { onClick(book) }
        }
    }
}
