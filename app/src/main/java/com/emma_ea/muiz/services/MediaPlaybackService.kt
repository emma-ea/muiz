package com.emma_ea.muiz.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.view.KeyEvent
import androidx.media.MediaBrowserServiceCompat
import com.emma_ea.muiz.R
import com.emma_ea.muiz.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

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

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {
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
    }

    private val mNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (mMediaPlayer == null && mMediaPlayer!!.isPlaying) mMediaSessionCallback.onPause()
        }
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        TODO("Not yet implemented")
    }

    fun setCurrentMetadata() {
        TODO("metadata associated with currently playing song")
    }


    fun showNotification(isPlaying: Boolean) {
        TODO("handle notification")
    }

}

