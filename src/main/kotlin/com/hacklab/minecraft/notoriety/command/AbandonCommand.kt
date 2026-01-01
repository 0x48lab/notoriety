package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.ownership.OwnershipService
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AbandonCommand(private val ownershipService: OwnershipService) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます")
            return true
        }

        // プレイヤーが見ているブロックを取得（最大5ブロック先まで）
        val targetBlock = player.getTargetBlockExact(5)
        if (targetBlock == null || targetBlock.type.isAir) {
            player.sendMessage("ブロックを見てからコマンドを実行してください")
            return true
        }

        val location = targetBlock.location
        val owner = ownershipService.getOwner(location)

        if (owner == null) {
            player.sendMessage("このブロックには所有者がいません")
            return true
        }

        if (owner != player.uniqueId) {
            player.sendMessage("あなたはこのブロックの所有者ではありません")
            return true
        }

        // 所有権を放棄
        ownershipService.removeOwnership(location)
        player.sendMessage("このブロックの所有権を放棄しました")

        return true
    }
}
