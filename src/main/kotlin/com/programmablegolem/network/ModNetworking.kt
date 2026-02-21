package com.programmablegolem.network

import com.programmablegolem.ProgrammableGolemMod
import com.programmablegolem.ai.BuildMode
import com.programmablegolem.ai.TaskType
import com.programmablegolem.blockentity.GolemComputerBlockEntity
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import java.util.*

data class ProgramGolemPayload(
    val blockPos: BlockPos,
    val taskType: TaskType,
    val blockName: String?,
    val toolName: String?,
    val buildMode: BuildMode?,
    val fromPos: BlockPos?,
    val toPos: BlockPos?,
    val anchorPos: BlockPos?,
    val schematicName: String?
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<ProgramGolemPayload>(
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, "program_golem")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ProgramGolemPayload> = object : StreamCodec<FriendlyByteBuf, ProgramGolemPayload> {
            override fun decode(buf: FriendlyByteBuf): ProgramGolemPayload {
                val pos = buf.readBlockPos()
                val task = TaskType.valueOf(buf.readUtf())
                val block = if (buf.readBoolean()) buf.readUtf() else null
                val tool = if (buf.readBoolean()) buf.readUtf() else null
                val mode = if (buf.readBoolean()) BuildMode.valueOf(buf.readUtf()) else null
                val from = if (buf.readBoolean()) buf.readBlockPos() else null
                val to = if (buf.readBoolean()) buf.readBlockPos() else null
                val anchor = if (buf.readBoolean()) buf.readBlockPos() else null
                val schem = if (buf.readBoolean()) buf.readUtf() else null
                return ProgramGolemPayload(pos, task, block, tool, mode, from, to, anchor, schem)
            }
            override fun encode(buf: FriendlyByteBuf, value: ProgramGolemPayload) {
                buf.writeBlockPos(value.blockPos)
                buf.writeUtf(value.taskType.name)
                buf.writeBoolean(value.blockName != null)
                value.blockName?.let { buf.writeUtf(it) }
                buf.writeBoolean(value.toolName != null)
                value.toolName?.let { buf.writeUtf(it) }
                buf.writeBoolean(value.buildMode != null)
                value.buildMode?.let { buf.writeUtf(it.name) }
                buf.writeBoolean(value.fromPos != null)
                value.fromPos?.let { buf.writeBlockPos(it) }
                buf.writeBoolean(value.toPos != null)
                value.toPos?.let { buf.writeBlockPos(it) }
                buf.writeBoolean(value.anchorPos != null)
                value.anchorPos?.let { buf.writeBlockPos(it) }
                buf.writeBoolean(value.schematicName != null)
                value.schematicName?.let { buf.writeUtf(it) }
            }
        }
    }
    override fun type() = TYPE
}

data class DisconnectCablePayload(
    val blockPos: BlockPos
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<DisconnectCablePayload>(
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, "disconnect_cable")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, DisconnectCablePayload> = object : StreamCodec<FriendlyByteBuf, DisconnectCablePayload> {
            override fun decode(buf: FriendlyByteBuf) = DisconnectCablePayload(buf.readBlockPos())
            override fun encode(buf: FriendlyByteBuf, value: DisconnectCablePayload) {
                buf.writeBlockPos(value.blockPos)
            }
        }
    }
    override fun type() = TYPE
}

data class OpenScreenPayload(
    val blockPos: BlockPos,
    val golemUUID: UUID?,
    val isDownloading: Boolean,
    val downloadProgress: Int
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<OpenScreenPayload>(
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, "open_screen")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, OpenScreenPayload> = object : StreamCodec<FriendlyByteBuf, OpenScreenPayload> {
            override fun decode(buf: FriendlyByteBuf): OpenScreenPayload {
                val pos = buf.readBlockPos()
                val hasUUID = buf.readBoolean()
                val uuid = if (hasUUID) buf.readUUID() else null
                val downloading = buf.readBoolean()
                val progress = buf.readInt()
                return OpenScreenPayload(pos, uuid, downloading, progress)
            }
            override fun encode(buf: FriendlyByteBuf, value: OpenScreenPayload) {
                buf.writeBlockPos(value.blockPos)
                buf.writeBoolean(value.golemUUID != null)
                value.golemUUID?.let { buf.writeUUID(it) }
                buf.writeBoolean(value.isDownloading)
                buf.writeInt(value.downloadProgress)
            }
        }
    }
    override fun type() = TYPE
}

object ModNetworking {
    fun registerServerPackets() {
        PayloadTypeRegistry.playC2S().register(ProgramGolemPayload.TYPE, ProgramGolemPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(DisconnectCablePayload.TYPE, DisconnectCablePayload.CODEC)
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.TYPE, OpenScreenPayload.CODEC)
        
        ServerPlayNetworking.registerGlobalReceiver(ProgramGolemPayload.TYPE) { payload, context ->
            context.server().execute {
                val player = context.player()
                val be = player.level().getBlockEntity(payload.blockPos)
                if (be is GolemComputerBlockEntity) {
                    be.selectedTask = payload.taskType
                    be.selectedBlockName = payload.blockName
                    be.selectedToolName = payload.toolName
                    be.selectedBuildMode = payload.buildMode
                    be.buildFromPos = payload.fromPos
                    be.buildToPos = payload.toPos
                    be.schematicAnchor = payload.anchorPos
                    be.schematicName = payload.schematicName
                    be.startDownload()
                }
            }
        }
        
        ServerPlayNetworking.registerGlobalReceiver(DisconnectCablePayload.TYPE) { payload, context ->
            context.server().execute {
                val player = context.player()
                val be = player.level().getBlockEntity(payload.blockPos)
                if (be is GolemComputerBlockEntity) {
                    be.disconnectGolem()
                }
            }
        }
    }
}
