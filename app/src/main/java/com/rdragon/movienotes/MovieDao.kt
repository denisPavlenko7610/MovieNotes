package com.rdragon.movienotes

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE watched = 0 ORDER BY createdAt DESC")
    fun getUnwatched(): LiveData<List<Movie>>

    @Query("SELECT * FROM movies WHERE name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchAll(query: String): LiveData<List<Movie>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<Movie>)

    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM movies")
    suspend fun clearAll()

    @Query("SELECT * FROM movies ORDER BY createdAt DESC")
    suspend fun getAllNow(): List<Movie>
}
