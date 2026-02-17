package com.programmablegolem

import com.programmablegolem.blocks.ModBlocks
import com.programmablegolem.blockentity.ModBlockEntities
import com.programmablegolem.entity.GolemComponent
import com.programmablegolem.items.ModItems
import com.programmablegolem.network.ModNetworking
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.animal.IronGolem
import org.slf4j.LoggerFactory

object ProgrammableGolemMod : ModInitializer {
    const val MOD_ID = "programmablegolem"
    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        logger.info("Initializing Programmable Iron Golems Mod")
        
        // Register items, blocks, and other content
        ModItems.register()
        ModBlocks.register()
        ModBlockEntities.register()
        ModNetworking.registerServerPackets()
        
        // Register event handlers
        registerEventHandlers()
        
        logger.info("Programmable Iron Golems Mod initialized successfully!")
    }
    
    private fun registerEventHandlers() {
        // Handle right-clicking iron golems with items
        UseEntityCallback.EVENT.register { player, world, hand, entity, hitResult ->
            if (entity is IronGolem && !world.isClientSide) {
                val stack = player.getItemInHand(hand)
                val component = GolemComponent.get(entity)
                
                when {
                    stack.`is`(ModItems.GOLEM_BRAIN) && !component.isUpgraded -> {
                        // Upgrade golem with brain
                        component.upgrade()
                        if (!player.abilities.instabuild) {
                            stack.shrink(1)
                        }
                        InteractionResult.SUCCESS
                    }
                    stack.`is`(ModItems.CONNECTION_CABLE) && component.isUpgraded -> {
                        // Connect cable to golem
                        component.connectCable(player.uuid)
                        if (!player.abilities.instabuild) {
                            stack.shrink(1)
                        }
                        InteractionResult.SUCCESS
                    }
                    else -> InteractionResult.PASS
                }
            } else {
                InteractionResult.PASS
            }
        }
    }
}
