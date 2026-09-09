package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.feature.media.domain.model.VoiceTranscriptCue
import com.cbgm.sparrow.feature.media.domain.model.VoiceTranscription

internal fun VoiceTranscription.toPersistedTranscript(): String =
    buildString {
        append(PREFIX)
        append(escape(text))
        cues.forEach { cue ->
            append(RECORD_SEPARATOR)
            append(cue.startMilliseconds)
            append(FIELD_SEPARATOR)
            append(cue.endMilliseconds)
            append(FIELD_SEPARATOR)
            append(escape(cue.text))
        }
    }

internal fun String.toVoiceTranscription(): VoiceTranscription {
    if (!startsWith(PREFIX)) return VoiceTranscription(text = this)

    val records = removePrefix(PREFIX).split(RECORD_SEPARATOR)
    val text = records.firstOrNull()?.let(::unescape).orEmpty()
    val cues =
        records.drop(1).mapNotNull { record ->
            val fields = record.split(FIELD_SEPARATOR, limit = 3)
            if (fields.size != 3) return@mapNotNull null
            val start = fields[0].toLongOrNull() ?: return@mapNotNull null
            val end = fields[1].toLongOrNull() ?: return@mapNotNull null
            val cueText = unescape(fields[2])
            if (cueText.isEmpty() || end <= start) return@mapNotNull null
            VoiceTranscriptCue(
                text = cueText,
                startMilliseconds = start,
                endMilliseconds = end
            )
        }

    return VoiceTranscription(text = text, cues = cues)
}

private fun escape(value: String): String =
    buildString(value.length) {
        value.forEach { character ->
            when (character) {
                ESCAPE -> append(ESCAPE).append('e')
                RECORD_SEPARATOR -> append(ESCAPE).append('r')
                FIELD_SEPARATOR -> append(ESCAPE).append('f')
                else -> append(character)
            }
        }
    }

private fun unescape(value: String): String =
    buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character == ESCAPE && index + 1 < value.length) {
                when (value[index + 1]) {
                    'e' -> append(ESCAPE)
                    'r' -> append(RECORD_SEPARATOR)
                    'f' -> append(FIELD_SEPARATOR)
                    else -> append(value[index + 1])
                }
                index += 2
            } else {
                append(character)
                index++
            }
        }
    }

private const val PREFIX = "\u0001sparrow-voice-transcript-v1\u0001"
private const val ESCAPE = '\u001d'
private const val RECORD_SEPARATOR = '\u001e'
private const val FIELD_SEPARATOR = '\u001f'
