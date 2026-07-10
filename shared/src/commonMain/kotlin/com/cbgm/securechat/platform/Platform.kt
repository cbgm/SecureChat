package com.cbgm.securechat.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform