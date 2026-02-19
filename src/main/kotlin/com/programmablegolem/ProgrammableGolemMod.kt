import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
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
        ModItems.register()
        ModBlocks.register()
        ModBlockEntities.register()
        ModNetworking.registerServerPackets()
        registerEventHandlers()
        logger.info("Done!")
    }

    private fun registerEventHandlers()
    registerGolemAI(){
        UseEntityCallback.EVENT.register { player, world, hand, entity, _ ->
            if (entity is IronGolem && !world.isClientSide) {
                val stack = player.getItemInHand(hand)
                val data = GolemComponent.get(entity)
                when {
                    stack.`is`(ModItems.GOLEM_BRAIN) && !data.isUpgraded -> {
                        data.isUpgraded = true
                        if (!player.abilities.instabuild) stack.shrink(1)
                        InteractionResult.SUCCESS
                    }
                    stack.`is`(ModItems.CONNECTION_CABLE) && data.isUpgraded -> {
                        data.isCabled = true
                        data.connectedPlayerUUID = player.uuid
                        if (!player.abilities.instabuild) stack.shrink(1)
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
private fun registerGolemAI() {
    ServerEntityEvents.ENTITY_LOAD.register { entity, _ ->
        if (entity is IronGolem) {
            val accessor = entity as? MobAccessor ?: return@register
            accessor.getGoalSelector().addGoal(1, GolemMiningGoal(entity))
            accessor.getGoalSelector().addGoal(1, GolemFightingGoal(entity))
            accessor.getGoalSelector().addGoal(1, GolemTradingGoal(entity))
            accessor.getGoalSelector().addGoal(1, GolemBuildingGoal(entity))
        }
    }
}
