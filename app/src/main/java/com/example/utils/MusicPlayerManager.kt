package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String,
    val audioUrl: String,
    val durationFormatted: String,
    val genre: String,
    val likesCount: Int = 1420
)

data class MusicPlayerState(
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val playlist: List<MusicTrack> = emptyList(),
    val favoriteTrackId: String? = null
)

object MusicPlayerManager {
    private const val TAG = "MusicPlayerManager"
    private const val PREFS_NAME = "iddet_music_prefs"
    private const val KEY_PROFILE_SONG_ID = "profile_favorite_song_id"

    val CURATED_TRACKS = listOf(
        MusicTrack(
            id = "track_lofi_sunset",
            title = "Midnight Lofi Coffee",
            artist = "Aura Chill & IDDET Beats",
            albumArt = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3",
            durationFormatted = "02:45",
            genre = "Lofi & Chill",
            likesCount = 3890
        ),
        MusicTrack(
            id = "track_afro_fusion",
            title = "Kinshasa Sunset Glow",
            artist = "K-Vibes & Afro Groove",
            albumArt = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=tropical-summer-112678.mp3",
            durationFormatted = "03:12",
            genre = "Afrobeats",
            likesCount = 5420
        ),
        MusicTrack(
            id = "track_synth_drive",
            title = "Cyber Horizon 2099",
            artist = "Neon Runner",
            albumArt = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=synthwave-80s-110045.mp3",
            durationFormatted = "03:30",
            genre = "Synthwave",
            likesCount = 4120
        ),
        MusicTrack(
            id = "track_piano_dreams",
            title = "Nocturne pour Étoiles",
            artist = "Luna Serenade",
            albumArt = "https://images.unsplash.com/photo-1520523839898-507127053c17?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2021/11/20/audio_c3c3a4f61f.mp3?filename=piano-moment-9835.mp3",
            durationFormatted = "02:18",
            genre = "Piano & Acoustique",
            likesCount = 2890
        ),
        MusicTrack(
            id = "track_deep_house",
            title = "Ibiza Deep Breeze",
            artist = "Solaris Club",
            albumArt = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f792cb.mp3?filename=deep-house-122977.mp3",
            durationFormatted = "03:45",
            genre = "Deep House",
            likesCount = 6120
        ),
        MusicTrack(
            id = "track_urban_flow",
            title = "Streetlights & Bass",
            artist = "Metro Flow",
            albumArt = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/08/02/audio_884fe92c21.mp3?filename=hip-hop-beat-118801.mp3",
            durationFormatted = "02:54",
            genre = "Hip-Hop",
            likesCount = 4780
        )
    )

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    private val _state = MutableStateFlow(
        MusicPlayerState(
            currentTrack = CURATED_TRACKS.first(),
            playlist = CURATED_TRACKS,
            favoriteTrackId = CURATED_TRACKS.first().id
        )
    )
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val favId = prefs.getString(KEY_PROFILE_SONG_ID, CURATED_TRACKS.first().id)
        _state.value = _state.value.copy(
            favoriteTrackId = favId
        )
    }

    fun setFavoriteTrackOnProfile(context: Context, track: MusicTrack) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE_SONG_ID, track.id).apply()
        _state.value = _state.value.copy(favoriteTrackId = track.id)
    }

    fun getFavoriteTrack(context: Context): MusicTrack {
        val favId = _state.value.favoriteTrackId ?: CURATED_TRACKS.first().id
        return CURATED_TRACKS.find { it.id == favId } ?: CURATED_TRACKS.first()
    }

    fun playTrack(track: MusicTrack) {
        if (_state.value.currentTrack?.id == track.id && mediaPlayer != null) {
            togglePlayPause()
            return
        }

        stopPlayback()
        _state.value = _state.value.copy(
            currentTrack = track,
            isPlaying = true,
            currentPositionMs = 0,
            durationMs = 0
        )

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(track.audioUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    _state.value = _state.value.copy(
                        isPlaying = true,
                        durationMs = mp.duration
                    )
                    startProgressTracking()
                }
                setOnCompletionListener {
                    if (_state.value.isRepeat) {
                        it.seekTo(0)
                        it.start()
                    } else {
                        nextTrack()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _state.value = _state.value.copy(isPlaying = false)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing music track", e)
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player == null) {
            val current = _state.value.currentTrack ?: CURATED_TRACKS.first()
            playTrack(current)
            return
        }

        try {
            if (player.isPlaying) {
                player.pause()
                _state.value = _state.value.copy(isPlaying = false)
                stopProgressTracking()
            } else {
                player.start()
                _state.value = _state.value.copy(isPlaying = true)
                startProgressTracking()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause", e)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _state.value = _state.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun nextTrack() {
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        val currentIndex = playlist.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val nextIndex = if (_state.value.isShuffle) {
            (playlist.indices).filter { it != currentIndex }.randomOrNull() ?: 0
        } else {
            (currentIndex + 1) % playlist.size
        }
        playTrack(playlist[nextIndex])
    }

    fun previousTrack() {
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        val currentIndex = playlist.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val prevIndex = if (currentIndex <= 0) playlist.size - 1 else currentIndex - 1
        playTrack(playlist[prevIndex])
    }

    fun toggleShuffle() {
        _state.value = _state.value.copy(isShuffle = !_state.value.isShuffle)
    }

    fun toggleRepeat() {
        _state.value = _state.value.copy(isRepeat = !_state.value.isRepeat)
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressRunnable = object : Runnable {
            override fun run() {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            _state.value = _state.value.copy(
                                currentPositionMs = player.currentPosition,
                                durationMs = player.duration
                            )
                            handler.postDelayed(this, 500)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressTracking() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    fun stopPlayback() {
        stopProgressTracking()
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }
        _state.value = _state.value.copy(isPlaying = false, currentPositionMs = 0)
    }
}
