package com.cbgm.securechat.main

enum class MainTab(
    val label: String,
    val symbol: String
) {
    Chats(
        label = "Chats",
        symbol = "●"
    ),
    Me(
        label = "Me",
        symbol = "◆"
    ),
    Settings(
        label = "Settings",
        symbol = "⚙"
    )
}