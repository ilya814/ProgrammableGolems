package com.programmablegolem.ai

import com.programmablegolem.entity.GolemComponent
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import java.util.EnumSet

class GolemMiningGoal(private val golem: IronGolem) : Goal() {
    private var targetPos: BlockPos? = null
    private var miningTicks = 0
    private val MINING_TIME = 40 // 2 seconds
    
    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }
    
    override fun canUse(): Boolean {
        val component = GolemComponent.get(golem)
        if (component.currentTask?.type != TaskType.MINING) return false
        
        // Find target block to mine
        targetPos = findNearestTargetBlock(component.currentTask?.targetBlockName)
        return targetPos != null
    }
    
    override fun canContinueToUse(): Boolean {
        return targetPos != null && miningTicks < MINING_TIME
    }
    
    override fun start() {
        miningTicks = 0
    }
    
    override fun tick() {
        targetPos?.let { pos ->
            // Look at the block
            golem.lookControl.setLookAt(
                pos.x.toDouble() + 0.5,
                pos.y.toDouble() + 0.5,
                pos.z.toDouble() + 0.5
            )
            
            // Move to the block if too far
            val distance = golem.blockPosition().distSqr(pos)
            if (distance > 4.0) {
                golem.navigation.moveTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), 1.0)
            } else {
                golem.navigation.stop()
                
                // Mine the block
                miningTicks++
                if (miningTicks >= MINING_TIME) {
                    golem.level().destroyBlock(pos, true, golem)
                    targetPos = null
                    miningTicks = 0
                }
            }
        }
    }
    
    override fun stop() {
        targetPos = null
        miningTicks = 0
    }
    
    private fun findNearestTargetBlock(blockName: String?): BlockPos? {
        if (blockName == null) return null
        
        val targetBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName))
        if (targetBlock == Blocks.AIR) return null
        
        val searchRadius = 16
        val golemPos = golem.blockPosition()
        
        for (x in -searchRadius..searchRadius) {
            for (y in -searchRadius..searchRadius) {
                for (z in -searchRadius..searchRadius) {
                    val pos = golemPos.offset(x, y, z)
                    if (golem.level().getBlockState(pos).`is`(targetBlock)) {
                        return pos
                    }
                }
            }
        }
        
        return null
    }
}
