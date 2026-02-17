package com.programmablegolem.network

import com.programmablegolem.ProgrammableGolemMod
import com.programmablegolem.ai.BuildMode
import com.programmablegolem.ai.TaskType
import com.programmablegolem.blockentity.GolemComputerBlockEntity
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

class ProgramGolemPayload(
    val computerPos: BlockPos,
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
        val CODEC: StreamCodec<FriendlyByteBuf, ProgramGolemPayload> =
            object : StreamCodec<FriendlyByteBuf, ProgramGolemPayload> {
                override fun decode(buf: FriendlyByteBuf) = ProgramGolemPayload(
                    buf.readBlockPos(),
                    TaskType.valueOf(buf.readUtf()),
                    if (buf.readBoolean()) buf.readUtf() else null,
                    if (buf.readBoolean()) buf.readUtf() else null,
                    if (buf.readBoolean()) BuildMode.valueOf(buf.readUtf()) else null,
                    if (buf.readBoolean()) buf.readBlockPos() else null,
                    if (buf.readBoolean()) buf.readBlockPos() else null,
                    if (buf.readBoolean()) buf.readBlockPos() else null,
                    if (buf.readBoolean()) buf.readUtf() else null
                )
                override fun encode(buf: FriendlyByteBuf, v: ProgramGolemPayload) {
                    buf.writeBlockPos(v.computerPos)
                    buf.writeUtf(v.taskType.name)
                    buf.writeBoolean(v.blockName != null); v.blockName?.let { buf.writeUtf(it) }
                    buf.writeBoolean(v.toolName != null); v.toolName?.let { buf.writeUtf(it) }
                    buf.writeBoolean(v.buildMode != null); v.buildMode?.let { buf.writeUtf(it.name) }
                    buf.writeBoolean(v.fromPos != null); v.fromPos?.let { buf.writeBlockPos(it) }
                    buf.writeBoolean(v.toPos != null); v.toPos?.let { buf.writeBlockPos(it) }
                    buf.writeBoolean(v.anchorPos != null); v.anchorPos?.let { buf.writeBlockPos(it) }
                    buf.writeBoolean(v.schematicName != null); v.schematicName?.let { buf.writeUtf(it) }
                }
            }
    }
    override fun type() = TYPE
}

class DisconnectCablePayload(val computerPos: BlockPos) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<DisconnectCablePayload>(
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, "disconnect_cable")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, DisconnectCablePayload> =
            object : StreamCodec<FriendlyByteBuf, DisconnectCablePayload> {
                override fun decode(buf: FriendlyByteBuf) = DisconnectCablePayload(buf.readBlockPos())
                override fun encode(buf: FriendlyByteBuf, v: DisconnectCablePayload) { buf.writeBlockPos(v.computerPos) }
            }
    }
    override fun type() = TYPE
}

class OpenScreenPayload(
    val computerPos: BlockPos,
    val isConnected: Boolean,
    val isDownloading: Boolean,
    val progress: Int
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<OpenScreenPayload>(
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, "open_screen")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, OpenScreenPayload> =
            object : StreamCodec<FriendlyByteBuf, OpenScreenPayload> {
                override fun decode(buf: FriendlyByteBuf) = OpenScreenPayload(buf.readBlockPos(), buf.readBoolean(), buf.readBoolean(), buf.readInt())
                override fun encode(buf: FriendlyByteBuf, v: OpenScreenPayload) {
                    buf.writeBlockPos(v.computerPos)
                    buf.writeBoolean(v.isConnected)
                    buf.writeBoolean(v.isDownloading)
                    buf.writeInt(v.progress)
                }
            }
    }
    override fun type() = TYPE
}

object ModNetworking {
    fun registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(ProgramGolemPayload.TYPE, ProgramGolemPayload.CODEC) { payload, context ->
            context.server().execute {
                val be = context.player().level().getBlockEntity(payload.computerPos)
                if (be is GolemComputerBlockEntity) {
                    be.startProgramming(
                        payload.taskType, payload.blockName, payload.toolName,
                        payload.buildMode, payload.fromPos, payload.toPos,
                        payload.anchorPos, payload.schematicName
                    )
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(DisconnectCablePayload.TYPE, DisconnectCablePayload.CODEC) { payload, context ->
            context.server().execute {
                val be = context.player().level().getBlockEntity(payload.computerPos)
                if (be is GolemComputerBlockEntity) be.disconnectGolem()
            }
        }
    }
}
