package com.example.notes_app.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApi {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 40,
        @Query("startIndex") startIndex: Int = 0,
        @Query("langRestrict") langRestrict: String = "en" // Default to English only
    ): Response<BookSearchResponse>
} 