package com.programmablegolem.blocks

import com.mojang.serialization.MapCodec
import com.programmablegolem.blockentity.GolemComputerBlockEntity
import com.programmablegolem.blockentity.ModBlockEntities
import com.programmablegolem.network.OpenScreenPayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class GolemComputerBlock(properties: Properties) : BaseEntityBlock(properties) {

    override fun codec(): MapCodec<out BaseEntityBlock> = MapCodec.unit(this)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return GolemComputerBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (level.isClientSide) {
            null
        } else {
            createTickerHelper(
                type,
                ModBlockEntities.GOLEM_COMPUTER
            ) { lvl, pos, st, be ->
                GolemComputerBlockEntity.tick(lvl, pos, st, be)
            }
        }
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide && player is ServerPlayer) {
            val be = level.getBlockEntity(pos)
            if (be is GolemComputerBlockEntity) {
                ServerPlayNetworking.send(
                    player,
                    OpenScreenPayload(
                        pos,
                        be.connectedGolemUUID,
                        be.isDownloading,
                        be.downloadProgress
                    )
                )
                return InteractionResult.SUCCESS
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }
}
