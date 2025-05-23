package com.example.notes_app.api

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GoogleBooksViewModel : ViewModel() {
    private val repository = GoogleBooksRepository()
    
    private val _searchResults = MutableLiveData<List<BookItem>>()
    val searchResults: LiveData<List<BookItem>> = _searchResults
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun searchBooks(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                val result = repository.searchBooks(query)
                result.onSuccess { books ->
                    _searchResults.value = books
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Unknown error occurred"
                    _searchResults.value = emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearSearch() {
        _searchResults.value = emptyList()
        _errorMessage.value = null
    }
} 