package com.cbgm.sparrow.feature.media.device

interface VoicePlayer {
    fun prepare(bytes: ByteArray): Result<Unit>

    fun play(bytes: ByteArray): Result<Unit>

    fun pause()

    fun resume()

    fun seekTo(positionMilliseconds: Long)

    fun stop()

    val isPlaying: Boolean

    val currentPositionMilliseconds: Long
}
