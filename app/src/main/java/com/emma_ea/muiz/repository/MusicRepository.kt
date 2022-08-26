package com.emma_ea.muiz.repository

import androidx.lifecycle.LiveData
import com.emma_ea.muiz.model.Song
import com.emma_ea.muiz.model.data.MusicDao

class MusicRepository(private val musicDao: MusicDao) {
    val allSongs: LiveData<List<Song>> = musicDao.getAlphabetizedSongs()

    suspend fun insertSong(song: Song) {
        musicDao.insert(song)
    }

    suspend fun updateSong(song: Song) {
        musicDao.updateMusicInfo(song)
    }

    suspend fun deleteSong(song: Song) {
        musicDao.delete(song)
    }
}