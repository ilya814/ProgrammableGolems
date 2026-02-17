package com.programmablegolem.items

import com.programmablegolem.ProgrammableGolemMod
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item

object ModItems {
    // Items
    val GOLEM_BRAIN = Item(Item.Properties())
    val CONNECTION_CABLE = Item(Item.Properties().stacksTo(16))
    val GOLEM_PICKAXE = Item(Item.Properties().stacksTo(1))
    val GOLEM_SWORD = Item(Item.Properties().stacksTo(1))
    val GOLEM_AXE = Item(Item.Properties().stacksTo(1))
    
    fun register() {
        // Register items
        registerItem("golem_brain", GOLEM_BRAIN)
        registerItem("connection_cable", CONNECTION_CABLE)
        registerItem("golem_pickaxe", GOLEM_PICKAXE)
        registerItem("golem_sword", GOLEM_SWORD)
        registerItem("golem_axe", GOLEM_AXE)
        
        // Add items to creative tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register { entries ->
            entries.accept(GOLEM_BRAIN)
            entries.accept(CONNECTION_CABLE)
            entries.accept(GOLEM_PICKAXE)
            entries.accept(GOLEM_SWORD)
            entries.accept(GOLEM_AXE)
        }
    }
    
    private fun registerItem(name: String, item: Item): Item {
        return Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ProgrammableGolemMod.MOD_ID, name),
            item
        )
    }
}
