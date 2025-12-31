package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.chat.command.ChatCommand
import com.hacklab.minecraft.notoriety.chat.service.ChatService
import org.bukkit.command.CommandSender

/**
 * ChatCommandを/notyのサブコマンドとしてラップするクラス
 */
class ChatCommandWrapper(chatService: ChatService) : SubCommand {

    private val chatCommand = ChatCommand(chatService)

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        return chatCommand.handleCommand(sender, args)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return chatCommand.handleTabComplete(sender, args)
    }
}
