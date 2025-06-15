package com.rdragon.movienotes

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = MovieDatabase.getInstance(application).movieDao()
    private val repo = MovieRepository(dao)

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
        val remoteId = FirebaseFirestore.getInstance().collection("__dummy__").document().id
        val movie = Movie(id = remoteId, name = name)
        dao.insertAll(listOf(movie))
        repo.syncMovieToRemote(movie)
    }

    fun updateMovie(movie: Movie) = viewModelScope.launch {
        dao.insertAll(listOf(movie))
        repo.syncMovieToRemote(movie)
    }

    fun deleteMovie(movie: Movie) = viewModelScope.launch {
        repo.deleteMovie(movie)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repo.syncToRemote()
        }
    }

    fun syncFromRemote() = viewModelScope.launch {
        repo.syncFromRemote()
    }

    fun syncToRemote() = viewModelScope.launch {
        repo.syncToRemote()
    }
}
