package com.rdragon.movienotes

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MovieRepository(private val dao: MovieDao) {
    private val TAG = "MovieRepository"
    private val db = FirebaseFirestore.getInstance()
    private val uid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not signed in")
    private val remoteRef
        get() = db.collection("users").document(uid).collection("movies")

    suspend fun syncFromRemote() {
        try {
            //Log.d(TAG, "syncFromRemote: start")
            dao.clearAll()

            val snapshot = remoteRef.get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Movie::class.java)
                    ?.copy(id = doc.id)
            }

            //Log.d(TAG, "syncFromRemote: fetched ${list.size} items → $list")
            dao.insertAll(list)
            //Log.d(TAG, "syncFromRemote: inserted into Room")
        } catch (e: Exception) {
            //Log.e(TAG, "syncFromRemote failed", e)
        }
    }

    suspend fun deleteMovie(movie: Movie) {
        try {
            remoteRef.document(movie.id).delete().await()
            //Log.d(TAG, "Deleted movie from remote: ${movie.id}")
        } catch (e: Exception) {
            //Log.e(TAG, "Failed to delete from remote", e)
        }

        try {
            dao.deleteById(movie.id)
            //Log.d(TAG, "Deleted movie from local Room: ${movie.id}")
        } catch (e: Exception) {
            //Log.e(TAG, "Failed to delete from Room", e)
        }
    }

    suspend fun syncToRemote() {
        try {
            //Log.d(TAG, "syncToRemote: start")
            val local = dao.getAllNow()  // возвращает List<Movie>
            //Log.d(TAG, "syncToRemote: local has ${local.size} items → $local")

            local.forEach { movie ->
                val map = mapOf(
                    "name" to movie.name,
                    "watched" to movie.watched
                )
                remoteRef.document(movie.id).set(map).await()
                //Log.d(TAG, "  synced remoteId=${movie.id}")
            }
            //Log.d(TAG, "syncToRemote: done")
        } catch (e: Exception) {
            //Log.e(TAG, "syncToRemote failed", e)
        }
    }

    suspend fun syncMovieToRemote(movie: Movie) {
        try {
            val map = mapOf("name" to movie.name, "watched" to movie.watched)
            remoteRef.document(movie.id).set(map).await()
            //Log.d(TAG, "Synced movie to remote: ${movie.id}")
        } catch (e: Exception) {
            //Log.e(TAG, "Failed to sync movie", e)
        }
    }
}