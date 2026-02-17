package com.programmablegolem.ai

import com.programmablegolem.entity.GolemComponent
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.npc.Villager
import java.util.EnumSet

class GolemTradingGoal(private val golem: IronGolem) : Goal() {
    private var targetVillager: Villager? = null
    private var tradingTicks = 0
    private val TRADING_TIME = 40 // 2 seconds to "trade"
    
    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }
    
    override fun canUse(): Boolean {
        val component = GolemComponent.get(golem)
        if (component.currentTask?.type != TaskType.TRADING) return false
        
        // Find nearest villager
        targetVillager = findNearestVillager()
        return targetVillager != null
    }
    
    override fun canContinueToUse(): Boolean {
        return targetVillager != null && targetVillager!!.isAlive && tradingTicks < TRADING_TIME
    }
    
    override fun start() {
        tradingTicks = 0
    }
    
    override fun tick() {
        targetVillager?.let { villager ->
            // Look at villager
            golem.lookControl.setLookAt(villager, 30.0f, 30.0f)
            
            val distance = golem.distanceToSqr(villager)
            
            // Move to villager if too far
            if (distance > 4.0) {
                golem.navigation.moveTo(villager, 1.0)
            } else {
                golem.navigation.stop()
                
                // Simulate trading
                tradingTicks++
                
                if (tradingTicks >= TRADING_TIME) {
                    // Trading complete - in a real implementation, 
                    // you'd handle actual item exchanges here
                    targetVillager = null
                    tradingTicks = 0
                }
            }
        }
    }
    
    override fun stop() {
        targetVillager = null
        tradingTicks = 0
    }
    
    private fun findNearestVillager(): Villager? {
        val searchRadius = 16.0
        return golem.level().getEntitiesOfClass(
            Villager::class.java,
            golem.boundingBox.inflate(searchRadius)
        ).minByOrNull { it.distanceToSqr(golem) }
    }
}
