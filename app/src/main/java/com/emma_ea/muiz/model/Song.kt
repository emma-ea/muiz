package com.emma_ea.muiz.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "music_table")
data class Song(
    @PrimaryKey val songID: Long,
    @ColumnInfo(name = "song_track") var track: Int,
    @ColumnInfo(name = "song_title") var title: String,
    @ColumnInfo(name = "song_artist") var artist: String,
    @ColumnInfo(name = "song_album") var album: String,
    @ColumnInfo(name = "song_album_id") val albumID: String,
    @ColumnInfo(name = "song_uri") val uri: String,
    @ColumnInfo(name = "song_year") var year: String,
    ) : Parcelable
