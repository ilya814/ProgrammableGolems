package com.programmablegolem.entity

import com.programmablegolem.ai.BuildMode
import com.programmablegolem.ai.GolemTask
import com.programmablegolem.ai.TaskType
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.animal.IronGolem
import java.util.UUID

class GolemComponent(private val golem: IronGolem) {
    var isUpgraded = false
        private set
    
    var isCabled = false
        private set
        
    var connectedPlayerUUID: UUID? = null
        private set
    
    var currentTask: GolemTask? = null
        private set
    
    var equippedWeapon: String? = null
    
    fun upgrade() {
        isUpgraded = true
        saveToNBT()
    }
    
    fun connectCable(playerUUID: UUID) {
        if (!isUpgraded) return
        isCabled = true
        connectedPlayerUUID = playerUUID
        saveToNBT()
    }
    
    fun disconnectCable() {
        isCabled = false
        connectedPlayerUUID = null
        saveToNBT()
    }
    
    fun programTask(task: GolemTask) {
        if (!isUpgraded) return
        currentTask = task
        disconnectCable()
        saveToNBT()
    }
    
    fun equipWeapon(weaponName: String) {
        if (!isUpgraded) return
        equippedWeapon = weaponName
        saveToNBT()
    }
    
    fun saveToNBT() {
        val tag = CompoundTag()
        tag.putBoolean("Upgraded", isUpgraded)
        tag.putBoolean("Cabled", isCabled)
        connectedPlayerUUID?.let { tag.putUUID("ConnectedPlayer", it) }
        
        currentTask?.let { task ->
            val taskTag = CompoundTag()
            taskTag.putString("Type", task.type.name)
            task.targetBlockName?.let { taskTag.putString("TargetBlock", it) }
            task.toolName?.let { taskTag.putString("Tool", it) }
            
            // Building-specific data
            task.buildMode?.let { taskTag.putString("BuildMode", it.name) }
            task.buildFromPos?.let { 
                taskTag.putLong("BuildFromPos", it.asLong())
            }
            task.buildToPos?.let { 
                taskTag.putLong("BuildToPos", it.asLong())
            }
            task.schematicAnchor?.let {
                taskTag.putLong("SchematicAnchor", it.asLong())
            }
            task.schematicName?.let { taskTag.putString("SchematicName", it) }
            
            tag.put("CurrentTask", taskTag)
        }
        
        equippedWeapon?.let { tag.putString("EquippedWeapon", it) }
        
        golem.persistentData.put(NBT_KEY, tag)
    }
    
    fun loadFromNBT() {
        val tag = golem.persistentData.getCompound(NBT_KEY)
        if (!tag.isEmpty) {
            isUpgraded = tag.getBoolean("Upgraded")
            isCabled = tag.getBoolean("Cabled")
            
            if (tag.hasUUID("ConnectedPlayer")) {
                connectedPlayerUUID = tag.getUUID("ConnectedPlayer")
            }
            
            if (tag.contains("CurrentTask")) {
                val taskTag = tag.getCompound("CurrentTask")
                val taskType = TaskType.valueOf(taskTag.getString("Type"))
                val targetBlock = if (taskTag.contains("TargetBlock")) {
                    taskTag.getString("TargetBlock")
                } else null
                val tool = if (taskTag.contains("Tool")) {
                    taskTag.getString("Tool")
                } else null
                
                // Building-specific data
                val buildMode = if (taskTag.contains("BuildMode")) {
                    BuildMode.valueOf(taskTag.getString("BuildMode"))
                } else null
                val buildFromPos = if (taskTag.contains("BuildFromPos")) {
                    BlockPos.of(taskTag.getLong("BuildFromPos"))
                } else null
                val buildToPos = if (taskTag.contains("BuildToPos")) {
                    BlockPos.of(taskTag.getLong("BuildToPos"))
                } else null
                val schematicAnchor = if (taskTag.contains("SchematicAnchor")) {
                    BlockPos.of(taskTag.getLong("SchematicAnchor"))
                } else null
                val schematicName = if (taskTag.contains("SchematicName")) {
                    taskTag.getString("SchematicName")
                } else null
                
                currentTask = GolemTask(
                    taskType, 
                    targetBlock, 
                    tool,
                    buildMode,
                    buildFromPos,
                    buildToPos,
                    schematicAnchor,
                    schematicName
                )
            }
            
            if (tag.contains("EquippedWeapon")) {
                equippedWeapon = tag.getString("EquippedWeapon")
            }
        }
    }
    
    companion object {
        private const val NBT_KEY = "programmablegolem_data"
        
        fun get(golem: IronGolem): GolemComponent {
            val component = GolemComponent(golem)
            component.loadFromNBT()
            return component
        }
    }
}
