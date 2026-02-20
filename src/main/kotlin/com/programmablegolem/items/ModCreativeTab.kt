package com.programmablegolem.items

import com.programmablegolem.ProgrammableGolemMod
import com.programmablegolem.blocks.ModBlocks
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack

object ModCreativeTab {
    val PROGRAMMABLE_GOLEMS_TAB: CreativeModeTab = FabricItemGroup.builder()
        .title(Component.translatable("itemGroup.programmablegolem.main"))
        .icon { ItemStack(ModItems.GOLEM_BRAIN) }
        .displayItems { _, output ->
            output.accept(ModItems.GOLEM_BRAIN)
            output.accept(ModItems.CONNECTION_CABLE)
            output.accept(ModBlocks.GOLEM_COMPUTER)
        }
        .build()

    fun register() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, "main"),
            PROGRAMMABLE_GOLEMS_TAB
        )
    }
}
