package com.example.movienotes

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MovieDao {
    @Query("SELECT * FROM Movie WHERE watched = 0 ORDER BY name")
    fun getUnwatched(): LiveData<List<Movie>>

    @Query("SELECT * FROM Movie WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchAll(query: String): LiveData<List<Movie>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(movie: Movie)

    @Update
    suspend fun update(movie: Movie)

    @Delete
    suspend fun delete(movie: Movie)
}