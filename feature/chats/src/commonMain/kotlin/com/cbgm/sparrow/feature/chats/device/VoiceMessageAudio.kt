package com.cbgm.sparrow.feature.chats.device

data class VoiceRecording(
    val bytes: ByteArray,
    val mimeType: String,
    val durationMilliseconds: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceRecording

        if (durationMilliseconds != other.durationMilliseconds) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = durationMilliseconds.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

interface VoiceMessageRecorder {
    suspend fun start(): Result<Unit>

    suspend fun stop(): Result<VoiceRecording>

    suspend fun cancel()
}

interface VoiceMessagePlayer {
    fun play(bytes: ByteArray): Result<Unit>

    fun pause()

    fun resume()

    fun stop()

    val isPlaying: Boolean

    val currentPositionMilliseconds: Long
}
