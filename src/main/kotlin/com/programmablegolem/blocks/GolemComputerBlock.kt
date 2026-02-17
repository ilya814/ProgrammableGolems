package com.programmablegolem.blocks

import com.programmablegolem.blockentity.GolemComputerBlockEntity
import com.programmablegolem.blockentity.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult

class GolemComputerBlock : Block(
    Properties.of()
        .mapColor(MapColor.METAL)
        .strength(3.5f)
        .sound(SoundType.METAL)
        .requiresCorrectToolForDrops()
), EntityBlock {
    
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return GolemComputerBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (level.isClientSide) {
            null
        } else {
            createTickerHelper(type, ModBlockEntities.GOLEM_COMPUTER) { world, pos, blockState, blockEntity ->
                blockEntity.tick(world, pos, blockState)
            }
        }
    }
    
    @Suppress("DEPRECATION")
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is GolemComputerBlockEntity && player is ServerPlayer) {
                // Send packet to open GUI
                val buf = io.netty.buffer.Unpooled.buffer()
                val packetBuf = net.minecraft.network.FriendlyByteBuf(buf)
                packetBuf.writeBlockPos(pos)
                packetBuf.writeBoolean(blockEntity.connectedGolemUUID != null)
                packetBuf.writeBoolean(blockEntity.isDownloading)
                packetBuf.writeInt(blockEntity.downloadProgress)
                
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    player,
                    com.programmablegolem.network.ModNetworking.OPEN_COMPUTER_SCREEN_PACKET,
                    packetBuf
                )
            }
        }
        return InteractionResult.SUCCESS
    }
    
    companion object {
        fun <E : BlockEntity, A : BlockEntity> createTickerHelper(
            givenType: BlockEntityType<A>,
            expectedType: BlockEntityType<E>,
            ticker: BlockEntityTicker<in E>
        ): BlockEntityTicker<A>? {
            return if (expectedType == givenType) {
                @Suppress("UNCHECKED_CAST")
                ticker as BlockEntityTicker<A>
            } else null
        }
    }
}
