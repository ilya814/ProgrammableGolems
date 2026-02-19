package com.programmablegolem.mixin

import com.programmablegolem.ai.GolemBuildingGoal
import com.programmablegolem.ai.GolemFightingGoal
import com.programmablegolem.ai.GolemMiningGoal
import com.programmablegolem.ai.GolemTradingGoal
import com.programmablegolem.entity.GolemComponent
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
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

    @Inject(method = ["<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V"], at = [At("RETURN")])
    private fun injectCustomGoals(ci: CallbackInfo) {
        val golem = this as IronGolem
        golem.goalSelector.addGoal(1, GolemMiningGoal(golem))
        golem.goalSelector.addGoal(1, GolemFightingGoal(golem))
        golem.goalSelector.addGoal(1, GolemTradingGoal(golem))
        golem.goalSelector.addGoal(1, GolemBuildingGoal(golem))
    }

    @Inject(method = ["addAdditionalSaveData"], at = [At("TAIL")])
    private fun saveGolemData(tag: CompoundTag, registries: HolderLookup.Provider, ci: CallbackInfo) {
        GolemComponent.saveToTag(this as IronGolem, tag, registries)
    }

    @Inject(method = ["readAdditionalSaveData"], at = [At("TAIL")])
    private fun loadGolemData(tag: CompoundTag, registries: HolderLookup.Provider, ci: CallbackInfo) {
        GolemComponent.loadFromTag(this as IronGolem, tag, registries)
    }
}
