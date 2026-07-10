package com.cbgm.securechat.domain.model

sealed class MessagePart {

    data class Text(
        val text: String
    ) : MessagePart()


    data class Image(
        val attachmentId: String
    ) : MessagePart()


    data class Video(
        val attachmentId: String
    ) : MessagePart()
}