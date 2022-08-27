package com.emma_ea.muiz.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.emma_ea.muiz.model.Song
import com.emma_ea.muiz.model.data.MusicDatabase
import com.emma_ea.muiz.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// we use AndroidViewModel so we can request application context
class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: MusicRepository
    val allSongs: LiveData<List<Song>>

    init {
        val musicDao = MusicDatabase.getDatabase(app, viewModelScope).musicDAO()
        repository = MusicRepository(musicDao)
        allSongs = repository.allSongs
    }

    fun deleteSong(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteSong(song)
    }

    fun updateMusicInfo(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateSong(song)
    }

    fun insert(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSong(song)
    }

}