package com.programmablegolem.blockentity

import com.programmablegolem.ProgrammableGolemMod
import com.programmablegolem.blocks.ModBlocks
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType

object ModBlockEntities {
    val GOLEM_COMPUTER: BlockEntityType<GolemComputerBlockEntity> = 
        BlockEntityType.Builder.of(
            ::GolemComputerBlockEntity,
            ModBlocks.GOLEM_COMPUTER
        ).build(null)
    
    fun register() {
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, "golem_computer"),
            GOLEM_COMPUTER
        )
    }
}
