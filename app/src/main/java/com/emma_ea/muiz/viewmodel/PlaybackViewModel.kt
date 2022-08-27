package com.emma_ea.muiz.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.emma_ea.muiz.model.Song

class PlaybackViewModel : ViewModel() {

    var currentPlayQueue = MutableLiveData<List<Pair<Int, Song>>>()
    var currentPlaybackDuration = MutableLiveData<Int>()
    var currentPlaybackPosition = MutableLiveData<Int>()
    var currentlyPlayingQueueID = MutableLiveData<Int>()
    var currentlyPlayingSong = MutableLiveData<Song?>()
    var isPlaying = MutableLiveData<Boolean>()

}