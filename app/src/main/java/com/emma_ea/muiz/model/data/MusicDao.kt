package com.emma_ea.muiz.model.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.emma_ea.muiz.model.Song

@Dao
interface MusicDao {
    @Query("SELECT * FROM music_table ORDER BY song_title ASC")
    fun getAlphabetizedSongs(): LiveData<List<Song>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inset(song: Song)

    @Query("SELECT * FROM music_table WHERE song_album_id LIKE :albumID LIMIT 10")
    suspend fun doesAlbumIDExist(albumID: String): List<Song>

    @Query("SELECT * FROM music_table WHERE song_title LIKE :search OR song_artist LIKE :search " +
            "OR song_album LIKE :search")
    suspend fun findBySearchSongs(search: String): List<Song>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMusicInfo(song: Song)

    @Delete
    suspend fun delete(song: Song)
}