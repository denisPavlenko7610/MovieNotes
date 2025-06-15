package com.rdragon.movienotes

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey val id: String = "",
    val name: String,
    var watched: Boolean = false
){
    constructor() : this("", "", false)
}
