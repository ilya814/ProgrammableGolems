package com.programmablegolem.ai

import com.programmablegolem.entity.GolemComponent
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.phys.AABB
import java.util.EnumSet

class GolemFightingGoal(private val golem: IronGolem) : Goal() {
    private var target: LivingEntity? = null
    private var attackCooldown = 0

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean {
        val component = GolemComponent.get(golem)
        if (component.currentTask?.type != TaskType.FIGHTING) return false
        target = findNearestMonster()
        return target != null && target!!.isAlive
    }

    override fun canContinueToUse(): Boolean {
        return target != null && target!!.isAlive
    }

    override fun start() {
        attackCooldown = 0
    }

    override fun tick() {
        target?.let { enemy ->
            if (!enemy.isAlive) {
                target = null
                return
            }
            golem.lookControl.setLookAt(enemy, 30.0f, 30.0f)
            val distance = golem.distanceToSqr(enemy)
            if (distance > 4.0) {
                golem.navigation.moveTo(enemy, 1.2)
            } else {
                golem.navigation.stop()
                if (attackCooldown <= 0) {
                    golem.doHurtTarget(enemy)
                    attackCooldown = 20
                }
            }
            if (attackCooldown > 0) attackCooldown--
        }
    }

    override fun stop() {
        target = null
        attackCooldown = 0
    }

    private fun findNearestMonster(): LivingEntity? {
        val searchBox = AABB(golem.blockPosition()).inflate(16.0)
        return golem.level().getEntitiesOfClass(Monster::class.java, searchBox)
            .minByOrNull { it.distanceToSqr(golem) }
    }
}
