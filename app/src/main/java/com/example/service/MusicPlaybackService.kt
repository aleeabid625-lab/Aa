package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.RecentPlayEntity
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MusicPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "abid_music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.abidmusic.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.abidmusic.ACTION_PAUSE"
        const val ACTION_TOGGLE = "com.example.abidmusic.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.example.abidmusic.ACTION_NEXT"
        const val ACTION_PREV = "com.example.abidmusic.ACTION_PREV"
        const val ACTION_STOP = "com.example.abidmusic.ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var playerManager: MusicPlayerManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        playerManager = MusicPlayerManager.getInstance(this)

        playerManager.onStateChanged = {
            updateNotification()
        }

        playerManager.onSongStarted = { song ->
            recordRecentPlay(song)
            updateNotification()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playerManager.resume()
            ACTION_PAUSE -> playerManager.pause()
            ACTION_TOGGLE -> playerManager.togglePlayPause()
            ACTION_NEXT -> playerManager.playNext()
            ACTION_PREV -> playerManager.playPrevious()
            ACTION_STOP -> {
                playerManager.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        updateNotification()
        return START_STICKY
    }

    private fun recordRecentPlay(song: Song) {
        serviceScope.launch {
            try {
                AppDatabase.getDatabase(applicationContext).musicDao().recordRecentPlay(
                    RecentPlayEntity(
                        songId = song.id,
                        songUri = song.contentUri,
                        songTitle = song.title,
                        songArtist = song.artist,
                        songAlbum = song.album,
                        durationMs = song.durationMs,
                        albumArtUri = song.albumArtUri,
                        playedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // Ignore DB error
            }
        }
    }

    private fun updateNotification() {
        val currentSong = playerManager.currentSong.value
        val isPlaying = playerManager.isPlaying.value

        val notification = buildNotification(currentSong, isPlaying)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(song: Song?, isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = song?.title ?: "Abid Music Player"
        val artist = song?.displayArtist ?: "Tap to play music"

        // Load album art or default logo
        val largeIconBitmap: Bitmap? = try {
            if (song?.albumArtUri != null) {
                contentResolver.openInputStream(Uri.parse(song.albumArtUri))?.use {
                    BitmapFactory.decodeStream(it)
                }
            } else {
                BitmapFactory.decodeResource(resources, R.drawable.ic_app_logo)
            }
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(song?.displayAlbum)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                togglePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Abid Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows now playing notification with playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
