package com.emma_ea.muiz

import android.content.ComponentName
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.view.Menu
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.emma_ea.muiz.databinding.ActivityMainBinding
import com.emma_ea.muiz.model.Song
import com.emma_ea.muiz.services.MediaPlaybackService
import com.emma_ea.muiz.viewmodel.PlaybackViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val channelID = "music"
    private var completeLibrary = listOf<Song>()
    private var currentlyPlayingQueueID = 0
    private var pbState = STATE_STOPPED
    private val playbackViewModel: PlaybackViewModel by viewModels()
    var playQueue = mutableListOf<Pair<Int, Song>>()
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var sharedPreferences: SharedPreferences

    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            mediaBrowser.sessionToken.also { token ->
                val mediaControllerCompat = MediaControllerCompat(this@MainActivity, token)
                mediaControllerCompat.registerCallback(controllerCallback)
                MediaControllerCompat.setMediaController(this@MainActivity, mediaControllerCompat)
            }
            MediaControllerCompat.getMediaController(this@MainActivity)
                .registerCallback(controllerCallback)
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            if (state == null) return
            when(state.state) {
                STATE_PLAYING -> {
                    pbState = state.state
                    val playbackPosition = state.position.toInt()
                    if (state.extras != null) {
                        val playbackDuration = state.extras!!.getInt("duration")
                        playbackViewModel.currentPlaybackDuration.value = playbackDuration
                    }
                    playbackViewModel.currentPlaybackPosition.value = playbackPosition
                    playbackViewModel.isPlaying.value = true
                }
                // TODO: handle states -> stopped, skipped to next, prev
                STATE_PAUSED -> {
                    pbState = state.state
                    val playbackPosition = state.position.toInt()
                    if (state.extras != null) {
                        val playbackDuration = state.extras!!.getInt("duration")
                        playbackViewModel.currentPlaybackDuration.value = playbackDuration
                    }
                    playbackViewModel.currentPlaybackPosition.value = playbackPosition
                    playbackViewModel.isPlaying.value = false
                }
                STATE_STOPPED -> {
                    pbState = state.state
                    playbackViewModel.isPlaying.value = false
                    playbackViewModel.currentPlayQueue.value = mutableListOf()
                    playbackViewModel.currentlyPlayingQueueID.value = 0
                    playbackViewModel.currentlyPlayingSong.value = null
                    playbackViewModel.currentPlaybackDuration.value = 0
                    playbackViewModel.currentPlaybackPosition.value = 0
                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // setup interaction with media browser service
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallback,
            intent.extras
        )
        mediaBrowser.connect()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}