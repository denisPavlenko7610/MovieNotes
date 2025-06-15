package com.RDragon.movienotes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Movie(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    var watched: Boolean = false
)
