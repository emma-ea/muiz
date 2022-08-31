package com.emma_ea.muiz.services

import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.media.MediaBrowserServiceCompat
import com.emma_ea.muiz.R
import com.emma_ea.muiz.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.NullPointerException

class MediaPlaybackService : MediaBrowserServiceCompat(), OnErrorListener {

    private val logTag = "AudioPlayer"
    private val channelID = "music"
    private var currentlyPlayingSong: Song? = null
    private val handler = Handler(Looper.getMainLooper())
    private var mMediaPlayer: MediaPlayer? = null
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var mMediaSessionCompat: MediaSessionCompat


    override fun onCreate() {
        super.onCreate()
        mMediaSessionCompat = MediaSessionCompat(baseContext, logTag).apply {
            setCallback(mMediaSessionCallback)
            setSessionToken(sessionToken)
        }
        val filter = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(mNoisyReceiver, filter)
        playbackPositionChecker.run()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            BrowserRoot(getString(R.string.app_name), null)
        } else null
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null)
    }

    private var playbackPositionChecker = object : Runnable {
        override fun run() {
            try {
                if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
                    val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition, 1f, null)
                }
            } finally {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private val afChangeListener: OnAudioFocusChangeListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT -> mMediaSessionCallback.onPause()
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mMediaPlayer != null) mMediaPlayer?.setVolume(0.3f, 0.3f)
            }
            AUDIOFOCUS_GAIN -> mMediaPlayer?.setVolume(1.0f, 1.0f)
        }
    }

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val ke = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (ke != null && mMediaPlayer != null) {
                when (ke.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (mMediaPlayer!!.isPlaying) onPause()
                        else onPlay()
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> onPlay()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> onPause()
                    KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> onSkipToNext()
                    KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> onSkipToPrevious()
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
            super.onPrepareFromUri(uri, extras)
            //TODO: path to media
            val bundle = extras!!.getString("song");
            val type = object : TypeToken<Song>(){}.type
            currentlyPlayingSong = Gson().fromJson(bundle, type)
            setCurrentMetadata()

            if (mMediaPlayer != null) mMediaPlayer!!.release()
            try {
                mMediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(application, Uri.parse(currentlyPlayingSong!!.uri))
                    setOnErrorListener(this@MediaPlaybackService)
                    prepare()
                    // refresh notification so user can see song has changed
                    showNotification(false)
                }
            } catch (e: IOException) {
                onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            } catch (e: IllegalStateException) {
                onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            } catch (e: IllegalArgumentException) {
                onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_MALFORMED)
            }

        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPlay() {
            super.onPlay()
            if (currentlyPlayingSong != null) {
                val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                // request audio focus for playback
                audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).run {
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setOnAudioFocusChangeListener(afChangeListener)
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        build()
                    })
                    build()
                }

                val result = audioManager.requestAudioFocus(audioFocusRequest)
                if (result == AUDIOFOCUS_REQUEST_GRANTED) {
                    // start service
                    startService(Intent(applicationContext, MediaBrowserService::class.java))
                    // set the session active
                    mMediaSessionCompat.isActive = true
                    showNotification(true)
                    try {
                        mMediaPlayer!!.start()
                        val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                        val playbackDuration = mMediaPlayer!!.duration
                        val bundle = Bundle()
                        bundle.putInt("duration", playbackDuration)
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition, 1F, bundle)
                        mMediaPlayer!!.setOnCompletionListener {
                            setMediaPlaybackState(
                                PlaybackStateCompat.STATE_SKIPPING_TO_NEXT,
                                0,
                                0F,
                                null
                            )
                        }
                    } catch (e: IllegalStateException) {
                        onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
                    } catch (e: NullPointerException) {
                        onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
                    }
                }

            }
        }

        override fun onPause() {
            super.onPause()
            if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
                mMediaPlayer!!.pause()
                val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                val playbackDuration = mMediaPlayer!!.duration
                val bundle = Bundle()
                bundle.putInt("duration", playbackDuration)
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition, 0f, bundle)
                showNotification(false)
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 0f, null)
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            // currentPosition returns the playback in milliseconds
            // if the media player is more than 5 seconds into song then restart song, otherwise skip back to previous song
            if (mMediaPlayer != null && mMediaPlayer!!.currentPosition > 5000) onSeekTo(0)
            else setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 0f, null)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onStop() {
            super.onStop()
            if (mMediaPlayer != null) {
                mMediaPlayer!!.stop()
                mMediaPlayer!!.release()
                mMediaPlayer = null
                currentlyPlayingSong = null
                stopForeground(true)
                try {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.abandonAudioFocusRequest(audioFocusRequest)
                } catch (ignore: UninitializedPropertyAccessException) { }
            }
            setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f, null)
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            mMediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    it.seekTo(pos.toInt())
                    it.start()
                    val playbackPosition = it.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition, 0f, null)
                } else {
                    val playbackPosition =it.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition, 0f, null)
                }
            }
        }

    }

    private val mNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (mMediaPlayer == null && mMediaPlayer!!.isPlaying) mMediaSessionCallback.onPause()
        }
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        mMediaPlayer?.reset()
        mMediaPlayer = null
        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L, 0F, null)
        currentlyPlayingSong = null
        stopForeground(true)
        Toast.makeText(application, getString(R.string.error), Toast.LENGTH_LONG).show()
        return true
    }

    private fun setMediaPlaybackState(state: Int, position: Long, playbackSpeed: Float, bundle: Bundle?) {
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, playbackSpeed)
            .setExtras(bundle)
            .build()
        mMediaSessionCompat.setPlaybackState(playbackState)
    }

    private fun setCurrentMetadata() {
        val metadata = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentlyPlayingSong?.title)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentlyPlayingSong?.artist)
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentlyPlayingSong?.album)
            putBitmap(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                getArtwork(currentlyPlayingSong?.albumID) ?:
                BitmapFactory.decodeResource(application.resources, R.drawable.ic_launcher_foreground))
        }.build()
        mMediaSessionCompat.setMetadata(metadata)
    }

    private fun getArtwork(albumArtwork: String?) : Bitmap? {
        try {
            return BitmapFactory.Options().run {
                inJustDecodeBounds = true
                val cw = ContextWrapper(applicationContext)
                val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
                val f = File(directory, "$albumArtwork.jpg")
                BitmapFactory.decodeStream(FileInputStream(f))
                inSampleSize = calculateSampleSize(this)
                inJustDecodeBounds = false
                BitmapFactory.decodeStream(FileInputStream(f))
            }
        } catch (ignore: FileNotFoundException) {}
        return null
    }

    private fun calculateSampleSize(options: BitmapFactory.Options): Int {
        return 0
    }



    fun showNotification(isPlaying: Boolean) {
        TODO("handle notification")
    }

}

