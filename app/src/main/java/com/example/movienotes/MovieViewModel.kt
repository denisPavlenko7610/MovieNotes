package com.example.movienotes

import android.app.Application
import androidx.lifecycle.*

import kotlinx.coroutines.launch

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = MovieDatabase.getInstance(application).movieDao()

    // LiveData со всем не просмотренным
    private val _searchQuery = MutableLiveData<String>("")
    val movies: LiveData<List<Movie>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) {
            dao.getUnwatched()
        } else {
            dao.searchUnwatched(query)
        }
    }

    /** Устанавливает строку поиска — автоматически обновит `movies`. */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** Загружает начальный список (просто подтолкнёт LiveData). */
    fun loadMovies() {
        _searchQuery.value = _searchQuery.value  // триггер повторного запроса
    }

    /** Добавляет новый фильм в базу. */
    fun addNewMovie(name: String) = viewModelScope.launch {
        dao.insert(Movie(name = name))
    }

    /** Обновляет статус watched. */
    fun updateMovie(movie: Movie) = viewModelScope.launch {
        dao.update(movie)
    }

    /** Удаляет фильм. */
    fun deleteMovie(movie: Movie) = viewModelScope.launch {
        dao.delete(movie)
    }
}
