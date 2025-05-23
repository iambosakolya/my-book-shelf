package com.example.notes_app.api

import android.util.Log

class GoogleBooksRepository {
    private val api = GoogleBooksService.googleBooksApi
    
    suspend fun searchBooks(query: String, startIndex: Int = 0): Result<List<BookItem>> {
        return try {
            // Exclude books with Russian language code in query
            val enhancedQuery = "$query -inlanguage:ru"
            val response = api.searchBooks(
                query = enhancedQuery,
                startIndex = startIndex,
                langRestrict = "" // No language restriction to allow all languages except Russian
            )
            
            if (response.isSuccessful) {
                val bookItems = response.body()?.items ?: listOf()
                
                // Second layer of filtering - exclude any books with Russian language code
                val filteredBooks = bookItems.filter { book ->
                    val language = book.volumeInfo?.language
                    language != "ru"
                }
                
                Result.success(filteredBooks)
            } else {
                Log.e("GoogleBooksRepository", "Error: ${response.errorBody()?.string()}")
                Result.failure(Exception("Search failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("GoogleBooksRepository", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
} 