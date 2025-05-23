package com.example.notes_app.api

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.notes_app.R

class BookAdapter(
    private val onBookSelected: (BookItem) -> Unit,
    private val onBookClicked: (BookItem) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
    
    private var books: List<BookItem> = emptyList()
    
    fun submitList(newBooks: List<BookItem>) {
        books = newBooks
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }
    
    override fun getItemCount() = books.size
    
    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewCover: ImageView = itemView.findViewById(R.id.imageViewBookCover)
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        private val textViewAuthors: TextView = itemView.findViewById(R.id.textViewAuthors)
        private val textViewPages: TextView = itemView.findViewById(R.id.textViewPages)
        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        private val btnAddBook: Button = itemView.findViewById(R.id.btnAddBook)
        
        fun bind(book: BookItem) {
            val volumeInfo = book.volumeInfo ?: return
            
            // Set book title
            textViewTitle.text = volumeInfo.title
            
            // Set authors
            textViewAuthors.text = volumeInfo.authors?.joinToString(", ") ?: "Unknown Author"
            
            // Set page count
            textViewPages.text = "Pages: ${volumeInfo.pageCount ?: "Unknown"}"
            
            // Set description
            textViewDescription.text = volumeInfo.description ?: "No description available"
            
            // Load book cover image
            val imageUrl = volumeInfo.imageLinks?.thumbnail?.replace("http:", "https:")
            Glide.with(itemView.context)
                .load(imageUrl)
                .apply(RequestOptions().centerCrop())
                .placeholder(R.drawable.ic_calendar) // Use existing drawable as placeholder
                .into(imageViewCover)
            
            // Set click listener for the add button
            btnAddBook.setOnClickListener {
                onBookSelected(book)
            }
            
            // Set click listener for the entire item
            itemView.setOnClickListener {
                onBookClicked(book)
            }
        }
    }
} 