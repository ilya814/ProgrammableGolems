package com.programmablegolem.blocks

import com.programmablegolem.ProgrammableGolemMod
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block

object ModBlocks {
    // Blocks
    val GOLEM_COMPUTER: Block = GolemComputerBlock(
    BlockBehaviour.Properties.of()
        .strength(3.5f)
        .requiresCorrectToolForDrops()
)
    
    fun register() {
        // Register blocks
        registerBlock("golem_computer", GOLEM_COMPUTER)
        
        // Add to creative tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register { entries ->
            entries.accept(GOLEM_COMPUTER)
        }
    }
    
    private fun registerBlock(name: String, block: Block): Block {
        val registered = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, name),
            block
        )
        Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, name),
            BlockItem(registered, Item.Properties())
        )
        return registered
    }
}
