package com.example.movienotes

import android.app.Application
import androidx.lifecycle.*

import kotlinx.coroutines.launch

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = MovieDatabase.getInstance(application).movieDao()

    private val _searchQuery = MutableLiveData<String>("")
    val movies: LiveData<List<Movie>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) {
            dao.getUnwatched()
        } else {
            dao.searchAll(query)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNewMovie(name: String) = viewModelScope.launch {
        dao.insert(Movie(name = name))
    }

    fun updateMovie(movie: Movie) = viewModelScope.launch {
        dao.update(movie)
    }

    fun deleteMovie(movie: Movie) = viewModelScope.launch {
        dao.delete(movie)
    }
}