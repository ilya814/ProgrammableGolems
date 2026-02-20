package com.programmablegolem.gui

import com.programmablegolem.blockentity.GolemComputerBlockEntity
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos

object ScreenOpener {
    fun openComputerScreen(pos: BlockPos) {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        val blockEntity = level.getBlockEntity(pos)
        
        if (blockEntity is GolemComputerBlockEntity) {
            minecraft.setScreen(
                context.client().setScreen(GolemComputerScreen(payload.blockPos, payload.golemUUID, payload.isDownloading, payload.downloadProgress))
                    pos,
                    blockEntity.connectedGolemUUID != null,
                    blockEntity.isDownloading,
                    blockEntity.downloadProgress
                )
            )
        }
    }
}
