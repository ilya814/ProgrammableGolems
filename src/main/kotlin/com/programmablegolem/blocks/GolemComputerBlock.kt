package com.programmablegolem.blocks

import com.mojang.serialization.MapCodec
import com.programmablegolem.blockentity.GolemComputerBlockEntity
import com.programmablegolem.blockentity.ModBlockEntities
import com.programmablegolem.entity.GolemComponent
import com.programmablegolem.network.OpenScreenPayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult

class GolemComputerBlock : BaseEntityBlock(BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()) {

    override fun codec(): MapCodec<out BaseEntityBlock> = MapCodec.unit(this)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        GolemComputerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (level.isClientSide) null
        else createTickerHelper(type, ModBlockEntities.GOLEM_COMPUTER) { lvl, pos, st, be ->
            GolemComputerBlockEntity.tick(lvl, pos, st, be)
        }
    }

    override fun getRenderShape(state: BlockState) = RenderShape.MODEL

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        val be = level.getBlockEntity(pos) as? GolemComputerBlockEntity ?: return InteractionResult.FAIL

        if (be.connectedGolemUUID == null) {
            val box = AABB(pos).inflate(10.0)
            val golem = level.getEntitiesOfClass(IronGolem::class.java, box).firstOrNull { g ->
                val comp = GolemComponent.get(g)
                comp.isCabled && comp.connectedPlayerUUID == player.uuid
            }
            if (golem != null) {
                be.connectedGolemUUID = golem.uuid
                be.setChanged()
            }
        }

        if (player is ServerPlayer) {
            ServerPlayNetworking.send(player, OpenScreenPayload(pos, blockEntity.connectedGolemUUID, blockEntity.isDownloading, blockEntity.downloadProgress))
        }
        return InteractionResult.SUCCESS
    }
}
