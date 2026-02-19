package com.programmablegolem.mixin

import com.programmablegolem.entity.GolemComponent
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.animal.IronGolem
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(IronGolem::class)
abstract class IronGolemMixin {

    @Inject(method = ["addAdditionalSaveData"], at = [At("TAIL")])
    private fun saveGolemData(tag: CompoundTag, registries: HolderLookup.Provider, ci: CallbackInfo) {
        GolemComponent.saveToTag(this as IronGolem, tag, registries)
    }

    @Inject(method = ["readAdditionalSaveData"], at = [At("TAIL")])
    private fun loadGolemData(tag: CompoundTag, registries: HolderLookup.Provider, ci: CallbackInfo) {
        GolemComponent.loadFromTag(this as IronGolem, tag, registries)
    }
}
