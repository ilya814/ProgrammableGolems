package com.programmablegolem.mixin

import com.programmablegolem.ai.GolemBuildingGoal
import com.programmablegolem.ai.GolemFightingGoal
import com.programmablegolem.ai.GolemMiningGoal
import com.programmablegolem.ai.GolemTradingGoal
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.AbstractGolem
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.level.Level
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(IronGolem::class)
abstract class IronGolemMixin(
    entityType: EntityType<out AbstractGolem>,
    level: Level
) : AbstractGolem(entityType, level) {

    @Inject(method = ["registerGoals"], at = [At("TAIL")])
    private fun injectCustomGoals(ci: CallbackInfo) {
        val golem = this as IronGolem
        goalSelector.addGoal(1, GolemMiningGoal(golem))
        goalSelector.addGoal(1, GolemFightingGoal(golem))
        goalSelector.addGoal(1, GolemTradingGoal(golem))
        goalSelector.addGoal(1, GolemBuildingGoal(golem))
    }
}
