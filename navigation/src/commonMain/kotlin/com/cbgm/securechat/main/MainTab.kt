package com.cbgm.securechat.main

import com.cbgm.securechat.core.ui.resources.SecureChatIcons
import org.jetbrains.compose.resources.DrawableResource


enum class MainTab(
    val label: String,
    val res: DrawableResource
) {
    Chats(
        label = "Chats",
        res = SecureChatIcons.ic_chats
    ),
    Me(
        label = "Me",
        res = SecureChatIcons.ic_identity
    ),
    Settings(
        label = "Settings",
        res = SecureChatIcons.ic_settings
    )
}