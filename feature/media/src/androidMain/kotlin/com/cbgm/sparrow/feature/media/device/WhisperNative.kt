package com.cbgm.sparrow.feature.media.device

internal class WhisperNative {
    external fun loadModel(modelPath: String): Long

    external fun transcribe(
        modelHandle: Long,
        samples: FloatArray
    ): String

    external fun freeModel(modelHandle: Long)

    private companion object {
        init {
            System.loadLibrary("sparrow_voice")
        }
    }
}
