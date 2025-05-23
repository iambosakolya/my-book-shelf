package com.example.notes_app.api

import com.google.gson.annotations.SerializedName

data class BookSearchResponse(
    @SerializedName("kind") val kind: String?,
    @SerializedName("totalItems") val totalItems: Int = 0,
    @SerializedName("items") val items: List<BookItem>? = null
)

data class BookItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo? = null
)

data class VolumeInfo(
    @SerializedName("title") val title: String = "",
    @SerializedName("authors") val authors: List<String>? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("publishedDate") val publishedDate: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("pageCount") val pageCount: Int? = null,
    @SerializedName("imageLinks") val imageLinks: ImageLinks? = null,
    @SerializedName("categories") val categories: List<String>? = null,
    @SerializedName("averageRating") val averageRating: Float? = null,
    @SerializedName("ratingsCount") val ratingsCount: Int? = null,
    @SerializedName("language") val language: String? = null
)

data class ImageLinks(
    @SerializedName("smallThumbnail") val smallThumbnail: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null
) 