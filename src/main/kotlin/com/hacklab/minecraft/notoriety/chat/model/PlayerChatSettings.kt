package com.hacklab.minecraft.notoriety.chat.model

import java.util.UUID

data class PlayerChatSettings(
    val playerUuid: UUID,
    val chatMode: ChatMode = ChatMode.LOCAL,
    val romajiEnabled: Boolean = false,
    val warningsEnabled: Boolean = true  // 犯罪警告のON/OFF（デフォルト: ON）
)
