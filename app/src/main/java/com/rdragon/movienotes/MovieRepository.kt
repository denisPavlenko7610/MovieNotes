package com.rdragon.movienotes

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MovieRepository(private val dao: MovieDao) {
    private val TAG = "MovieRepository"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun syncFromRemote() {
        val user = getCurrentUser() ?: return
        if (user.isAnonymous) return

        try {
            dao.clearAll()
            val remoteRef = db.collection("users").document(user.uid).collection("movies")
            val snapshot = remoteRef.get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Movie::class.java)?.copy(id = doc.id)
            }
            dao.insertAll(list)
        } catch (e: Exception) {
        }
    }

    suspend fun syncMovieToRemote(movie: Movie) {
        val user = getCurrentUser() ?: return
        if (user.isAnonymous) return

        try {
            val remoteRef = db.collection("users").document(user.uid).collection("movies")
            val map = mapOf("name" to movie.name, "watched" to movie.watched)
            remoteRef.document(movie.id).set(map).await()
        } catch (e: Exception) {
        }
    }

    suspend fun deleteMovie(movie: Movie) {
        val user = getCurrentUser()
        try {
            if (user != null && !user.isAnonymous) {
                val remoteRef = db.collection("users").document(user.uid).collection("movies")
                remoteRef.document(movie.id).delete().await()
            }
            dao.deleteById(movie.id)
        } catch (e: Exception) {
        }
    }
}