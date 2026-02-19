package com.programmablegolem.entity

import com.programmablegolem.ai.BuildMode
import com.programmablegolem.ai.GolemTask
import com.programmablegolem.ai.TaskType
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.item.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GolemData {
    var isUpgraded = false
    var isCabled = false
    var connectedPlayerUUID: UUID? = null
    var currentTask: GolemTask? = null
    val inventory: NonNullList<ItemStack> = NonNullList.withSize(72, ItemStack.EMPTY)
}

object GolemComponent {
    private val dataMap = ConcurrentHashMap<UUID, GolemData>()

    fun get(golem: IronGolem): GolemData {
        return dataMap.getOrPut(golem.uuid) { GolemData() }
    }

    fun saveToTag(golem: IronGolem, tag: CompoundTag, registries: HolderLookup.Provider) {
        val data = dataMap[golem.uuid] ?: return
        val t = CompoundTag()
        t.putBoolean("Upgraded", data.isUpgraded)
        t.putBoolean("Cabled", data.isCabled)
        data.connectedPlayerUUID?.let { t.putUUID("ConnectedPlayer", it) }
        ContainerHelper.saveAllItems(t, data.inventory, registries)
        
        data.currentTask?.let { task ->
            val taskTag = CompoundTag()
            taskTag.putString("Type", task.type.name)
            task.targetBlockName?.let { taskTag.putString("TargetBlock", it) }
            task.toolName?.let { taskTag.putString("Tool", it) }
            task.buildMode?.let { taskTag.putString("BuildMode", it.name) }
            task.buildFromPos?.let { taskTag.putLong("BuildFromPos", it.asLong()) }
            task.buildToPos?.let { taskTag.putLong("BuildToPos", it.asLong()) }
            task.schematicAnchor?.let { taskTag.putLong("SchematicAnchor", it.asLong()) }
            task.schematicName?.let { taskTag.putString("SchematicName", it) }
            t.put("Task", taskTag)
        }
        tag.put("ProgrammableGolem", t)
    }

    fun loadFromTag(golem: IronGolem, tag: CompoundTag, registries: HolderLookup.Provider) {
        if (!tag.contains("ProgrammableGolem")) return
        val t = tag.getCompound("ProgrammableGolem")
        val data = GolemData().apply {
            isUpgraded = t.getBoolean("Upgraded")
            isCabled = t.getBoolean("Cabled")
            if (t.hasUUID("ConnectedPlayer")) connectedPlayerUUID = t.getUUID("ConnectedPlayer")
            ContainerHelper.loadAllItems(t, inventory, registries)
            
            if (t.contains("Task")) {
                val taskTag = t.getCompound("Task")
                val type = TaskType.valueOf(taskTag.getString("Type"))
                currentTask = GolemTask(
                    type = type,
                    targetBlockName = if (taskTag.contains("TargetBlock")) taskTag.getString("TargetBlock") else null,
                    toolName = if (taskTag.contains("Tool")) taskTag.getString("Tool") else null,
                    buildMode = if (taskTag.contains("BuildMode")) BuildMode.valueOf(taskTag.getString("BuildMode")) else null,
                    buildFromPos = if (taskTag.contains("BuildFromPos")) BlockPos.of(taskTag.getLong("BuildFromPos")) else null,
                    buildToPos = if (taskTag.contains("BuildToPos")) BlockPos.of(taskTag.getLong("BuildToPos")) else null,
                    schematicAnchor = if (taskTag.contains("SchematicAnchor")) BlockPos.of(taskTag.getLong("SchematicAnchor")) else null,
                    schematicName = if (taskTag.contains("SchematicName")) taskTag.getString("SchematicName") else null
                )
            }
        }
        dataMap[golem.uuid] = data
    }
    
    fun addItemToInventory(golem: IronGolem, stack: ItemStack): Boolean {
        val data = get(golem)
        var remaining = stack.copy()
        
        // Try merging with existing stacks
        for (i in 0 until data.inventory.size) {
            val slot = data.inventory[i]
            if (slot.isEmpty) continue
            if (ItemStack.isSameItemSameComponents(slot, stack)) {
                val space = slot.maxStackSize - slot.count
                if (space > 0) {
                    val toAdd = minOf(space, remaining.count)
                    slot.grow(toAdd)
                    remaining.shrink(toAdd)
                    if (remaining.isEmpty) return true
                }
            }
        }
        
        // Add to empty slots
        for (i in 0 until data.inventory.size) {
            if (data.inventory[i].isEmpty) {
                data.inventory[i] = remaining
                return true
            }
        }
        
        return false
    }
}
