package com.cbgm.securechat.core.extensions

/**
 * Converts binary data into lowercase hexadecimal text.
 *
 * Example:
 *
 * byteArrayOf(0x01, 0x2A)
 *
 * becomes:
 *
 * 012a
 */
fun ByteArray.toHexString(): String {
    return joinToString(
        separator = ""
    ) { byte ->
        byte
            .toUByte()
            .toString(radix = 16)
            .padStart(
                length = 2,
                padChar = '0'
            )
    }
}

/**
 * Converts hexadecimal text back into binary data.
 *
 * Example:
 *
 * 012a
 *
 * becomes:
 *
 * byteArrayOf(0x01, 0x2A)
 */
fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) {
        "Hexadecimal value has an invalid length"
    }

    return ByteArray(
        size = length / 2
    ) { index ->

        val startIndex = index * 2

        substring(
            startIndex = startIndex,
            endIndex = startIndex + 2
        )
            .toInt(radix = 16)
            .toByte()
    }
}