package com.cbgm.securechat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform