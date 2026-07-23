package com.cbgm.securechat.presentation.model

import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.ic_chat
import com.cbgm.securechat.resources.ic_chat_outlined
import com.cbgm.securechat.resources.ic_identity
import com.cbgm.securechat.resources.ic_identity_outlined
import com.cbgm.securechat.resources.ic_settings
import com.cbgm.securechat.resources.ic_settings_outlined
import org.jetbrains.compose.resources.DrawableResource

enum class MainTab(
    val label: String,
    val res: DrawableResource,
    val resOutlined: DrawableResource,
) {
    Chats(
        label = "Chats",
        res = Res.drawable.ic_chat,
        resOutlined = Res.drawable.ic_chat_outlined,
    ),
    Me(
        label = "Me",
        res = Res.drawable.ic_identity,
        resOutlined = Res.drawable.ic_identity_outlined,
    ),
    Settings(
        label = "Settings",
        res = Res.drawable.ic_settings,
        resOutlined = Res.drawable.ic_settings_outlined,
    ),
}
