package com.cbgm.sparrow.feature.media.device

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.media.domain.repository.VoiceTranscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.floor

internal class AndroidVoiceTranscriptionRepository(
    private val modelStore: AndroidWhisperModelStore
) : VoiceTranscriptionRepository {
    private val whisperNative by lazy(::WhisperNative)
    private val transcriptionMutex = Mutex()
    private var modelHandle: Long = 0L

    override suspend fun transcribe(bytes: ByteArray): Result<String> = safeSuspendCall {
        require(bytes.isNotEmpty()) { "Voice message is empty" }

        val wave = bytes.toPcmWaveAudio()
        val samples = wave.toWhisperSamples()
        val modelFile = withContext(Dispatchers.IO) { modelStore.requireModel() }

        withContext(Dispatchers.Default) {
            transcriptionMutex.withLock {
                val handle = requireModelHandle(modelFile.absolutePath)
                whisperNative
                    .transcribe(handle, samples)
                    .trim()
                    .also { transcript ->
                        check(transcript.isNotBlank()) { "No speech could be transcribed" }
                    }
            }
        }
    }

    private fun requireModelHandle(modelPath: String): Long {
        if (modelHandle != 0L) return modelHandle

        modelHandle = whisperNative.loadModel(modelPath)
        check(modelHandle != 0L) { "Could not load the voice transcription model" }
        return modelHandle
    }
}

private fun PcmWaveAudio.toWhisperSamples(): FloatArray {
    val sourceSampleCount = pcmBytes.size / PCM_BYTES_PER_SAMPLE
    val source = FloatArray(sourceSampleCount) { index ->
        val byteOffset = index * PCM_BYTES_PER_SAMPLE
        val sample =
            (
                (pcmBytes[byteOffset].toInt() and 0xff) or
                    ((pcmBytes[byteOffset + 1].toInt() and 0xff) shl 8)
            )
                .toShort()
        sample.toFloat() / 32768f
    }

    if (sampleRate == WHISPER_SAMPLE_RATE_HZ) return source

    val targetSampleCount =
        ((source.size.toLong() * WHISPER_SAMPLE_RATE_HZ) / sampleRate)
            .toInt()
            .coerceAtLeast(1)
    val sourceStep = sampleRate.toDouble() / WHISPER_SAMPLE_RATE_HZ.toDouble()

    return FloatArray(targetSampleCount) { targetIndex ->
        val sourcePosition = targetIndex * sourceStep
        val leftIndex = floor(sourcePosition).toInt().coerceIn(0, source.lastIndex)
        val rightIndex = (leftIndex + 1).coerceAtMost(source.lastIndex)
        val fraction = (sourcePosition - leftIndex).toFloat()
        source[leftIndex] + (source[rightIndex] - source[leftIndex]) * fraction
    }
}

private const val PCM_BYTES_PER_SAMPLE = 2
private const val WHISPER_SAMPLE_RATE_HZ = 16_000
