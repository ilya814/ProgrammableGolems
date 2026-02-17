package com.programmablegolem.network

import com.programmablegolem.ProgrammableGolemMod
import com.programmablegolem.ai.BuildMode
import com.programmablegolem.ai.TaskType
import com.programmablegolem.blockentity.GolemComputerBlockEntity
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation

object ModNetworking {
    val PROGRAM_GOLEM_PACKET = ResourceLocation.fromNamespaceAndPath(
        ProgrammableGolemMod.MOD_ID, 
        "program_golem"
    )
    
    val DISCONNECT_CABLE_PACKET = ResourceLocation.fromNamespaceAndPath(
        ProgrammableGolemMod.MOD_ID,
        "disconnect_cable"
    )
    
    val OPEN_COMPUTER_SCREEN_PACKET = ResourceLocation.fromNamespaceAndPath(
        ProgrammableGolemMod.MOD_ID,
        "open_computer_screen"
    )
    
    fun registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(
            PROGRAM_GOLEM_PACKET
        ) { server, player, handler, buf, responseSender ->
            val pos = buf.readBlockPos()
            val taskType = TaskType.valueOf(buf.readUtf())
            val hasBlock = buf.readBoolean()
            val blockName = if (hasBlock) buf.readUtf() else null
            val hasTool = buf.readBoolean()
            val toolName = if (hasTool) buf.readUtf() else null
            
            // Read building-specific data
            val hasBuildMode = buf.readBoolean()
            val buildMode = if (hasBuildMode) BuildMode.valueOf(buf.readUtf()) else null
            var fromPos: BlockPos? = null
            var toPos: BlockPos? = null
            var anchorPos: BlockPos? = null
            var schematicName: String? = null
            
            if (taskType == TaskType.BUILDING && buildMode != null) {
                fromPos = buf.readBlockPos()
                toPos = buf.readBlockPos()
                val hasAnchor = buf.readBoolean()
                if (hasAnchor) {
                    anchorPos = buf.readBlockPos()
                }
                val hasSchematic = buf.readBoolean()
                if (hasSchematic) {
                    schematicName = buf.readUtf()
                }
            }
            
            server.execute {
                val blockEntity = player.level().getBlockEntity(pos)
                if (blockEntity is GolemComputerBlockEntity) {
                    blockEntity.startProgramming(
                        taskType, 
                        blockName, 
                        toolName,
                        buildMode,
                        fromPos,
                        toPos,
                        anchorPos,
                        schematicName
                    )
                }
            }
        }
        
        ServerPlayNetworking.registerGlobalReceiver(
            DISCONNECT_CABLE_PACKET
        ) { server, player, handler, buf, responseSender ->
            val pos = buf.readBlockPos()
            
            server.execute {
                val blockEntity = player.level().getBlockEntity(pos)
                if (blockEntity is GolemComputerBlockEntity) {
                    blockEntity.disconnectGolem()
                }
            }
        }
    }
    
    fun registerClientPackets() {
        // Register client-side packet receiver for opening screen
        ClientPlayNetworking.registerGlobalReceiver(OPEN_COMPUTER_SCREEN_PACKET) { client, handler, buf, responseSender ->
            val pos = buf.readBlockPos()
            val isConnected = buf.readBoolean()
            val isDownloading = buf.readBoolean()
            val downloadProgress = buf.readInt()
            
            client.execute {
                val screen = com.programmablegolem.gui.GolemComputerScreen(
                    pos, isConnected, isDownloading, downloadProgress
                )
                Minecraft.getInstance().setScreen(screen)
            }
        }
    }
}
