package com.cbgm.securechat

import com.cbgm.securechat.platform.getPlatform

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}