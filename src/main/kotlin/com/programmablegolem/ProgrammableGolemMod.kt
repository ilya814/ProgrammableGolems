package com.programmablegolem

import com.programmablegolem.ai.*
import com.programmablegolem.blocks.ModBlocks
import com.programmablegolem.blockentity.ModBlockEntities
import com.programmablegolem.entity.GolemComponent
import com.programmablegolem.items.ModCreativeTab
import com.programmablegolem.items.ModItems
import com.programmablegolem.network.ModNetworking
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.animal.IronGolem
import org.slf4j.LoggerFactory

object ProgrammableGolemMod : ModInitializer {
    const val MOD_ID = "programmablegolem"
    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        logger.info("Initializing Programmable Iron Golems Mod")
        ModItems.register()
        ModCreativeTab.register()
        ModBlocks.register()
        ModBlockEntities.register()
        ModNetworking.registerServerPackets()
        registerEventHandlers()
        registerGolemAI()
        logger.info("Done!")
    }

    private fun registerEventHandlers() {
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
                    stack.`is`(ModItems.CONNECTION_CABLE) && data.isUpgraded && !data.isCabled -> {
                        data.isCabled = true
                        data.connectedPlayerUUID = player.uuid
                        entity.setNoAi(true)
                        if (!player.abilities.instabuild) stack.shrink(1)
                        player.sendSystemMessage(Component.literal("§aGolem connected! Find a Golem Computer to program it."))
                        InteractionResult.SUCCESS
                    }
                    else -> InteractionResult.PASS
                }
            } else {
                InteractionResult.PASS
            }
        }
    }

    private fun registerGolemAI() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity is IronGolem && !world.isClientSide) {
                val data = GolemComponent.get(entity)
                if (data.isUpgraded && !data.isCabled) {
                    try {
                        val goalSelectorField = entity.javaClass.superclass.superclass.getDeclaredField("goalSelector")
                        goalSelectorField.isAccessible = true
                        val goalSelector = goalSelectorField.get(entity) as net.minecraft.world.entity.ai.goal.GoalSelector
                        
                        goalSelector.addGoal(1, GolemMiningGoal(entity))
                        goalSelector.addGoal(1, GolemFightingGoal(entity))
                        goalSelector.addGoal(1, GolemTradingGoal(entity))
                        goalSelector.addGoal(1, GolemBuildingGoal(entity))
                    } catch (e: Exception) {
                        logger.error("Failed to add golem AI goals", e)
                    }
                }
            }
        }
    }
}
