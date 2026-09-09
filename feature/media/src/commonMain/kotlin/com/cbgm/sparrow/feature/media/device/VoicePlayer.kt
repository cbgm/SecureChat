package com.cbgm.sparrow.feature.media.device

interface VoicePlayer {
    fun play(bytes: ByteArray): Result<Unit>

    fun pause()

    fun resume()

    fun stop()

    val isPlaying: Boolean

    val currentPositionMilliseconds: Long
}
