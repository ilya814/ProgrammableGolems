package com.programmablegolem.blockentity

import com.programmablegolem.ai.BuildMode
import com.programmablegolem.ai.GolemTask
import com.programmablegolem.ai.TaskType
import com.programmablegolem.entity.GolemComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import java.util.UUID

class GolemComputerBlockEntity(pos: BlockPos, state: BlockState) : 
    BlockEntity(ModBlockEntities.GOLEM_COMPUTER, pos, state), MenuProvider {
    
    var connectedGolemUUID: UUID? = null
    var isDownloading = false
    var downloadProgress = 0
    private val downloadTime = 60 // 3 seconds at 20 ticks/sec
    
    var selectedTask: TaskType? = null
    var selectedBlockName: String? = null
    var selectedToolName: String? = null
    
    // Building-specific fields
    var selectedBuildMode: BuildMode? = null
    var buildFromPos: BlockPos? = null
    var buildToPos: BlockPos? = null
    var schematicAnchor: BlockPos? = null
    var schematicName: String? = null
    
    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClientSide) return
        
        // Handle downloading process
        if (isDownloading) {
            downloadProgress++
            if (downloadProgress >= downloadTime) {
                completeDownload()
            }
            setChanged()
        }
    }
    
    fun startProgramming(
        taskType: TaskType, 
        blockName: String?, 
        toolName: String?,
        buildMode: BuildMode? = null,
        fromPos: BlockPos? = null,
        toPos: BlockPos? = null,
        anchorPos: BlockPos? = null,
        schematic: String? = null
    ) {
        selectedTask = taskType
        selectedBlockName = blockName
        selectedToolName = toolName
        selectedBuildMode = buildMode
        buildFromPos = fromPos
        buildToPos = toPos
        schematicAnchor = anchorPos
        schematicName = schematic
        isDownloading = true
        downloadProgress = 0
        setChanged()
    }
    
    private fun completeDownload() {
        isDownloading = false
        downloadProgress = 0
        
        // Program the connected golem
        connectedGolemUUID?.let { uuid ->
            level?.let { world ->
                val golem = findGolemByUUID(world, uuid)
                golem?.let {
                    val component = GolemComponent.get(it)
                    selectedTask?.let { task ->
                        component.programTask(
                            GolemTask(
                                task, 
                                selectedBlockName, 
                                selectedToolName,
                                selectedBuildMode,
                                buildFromPos,
                                buildToPos,
                                schematicAnchor,
                                schematicName
                            )
                        )
                    }
                }
            }
        }
        
        setChanged()
    }
    
    fun disconnectGolem() {
        connectedGolemUUID = null
        isDownloading = false
        downloadProgress = 0
        selectedTask = null
        selectedBlockName = null
        selectedToolName = null
        selectedBuildMode = null
        buildFromPos = null
        buildToPos = null
        schematicAnchor = null
        schematicName = null
        setChanged()
    }
    
    private fun findGolemByUUID(world: Level, uuid: UUID): IronGolem? {
        val searchBox = AABB(blockPos).inflate(10.0)
        return world.getEntitiesOfClass(IronGolem::class.java, searchBox)
            .firstOrNull { it.uuid == uuid }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        connectedGolemUUID?.let { tag.putUUID("ConnectedGolem", it) }
        tag.putBoolean("IsDownloading", isDownloading)
        tag.putInt("DownloadProgress", downloadProgress)
        selectedTask?.let { tag.putString("SelectedTask", it.name) }
        selectedBlockName?.let { tag.putString("SelectedBlock", it) }
        selectedToolName?.let { tag.putString("SelectedTool", it) }
        
        // Save building-specific data
        selectedBuildMode?.let { tag.putString("BuildMode", it.name) }
        buildFromPos?.let { tag.putLong("BuildFromPos", it.asLong()) }
        buildToPos?.let { tag.putLong("BuildToPos", it.asLong()) }
        schematicAnchor?.let { tag.putLong("SchematicAnchor", it.asLong()) }
        schematicName?.let { tag.putString("SchematicName", it) }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.hasUUID("ConnectedGolem")) {
            connectedGolemUUID = tag.getUUID("ConnectedGolem")
        }
        isDownloading = tag.getBoolean("IsDownloading")
        downloadProgress = tag.getInt("DownloadProgress")
        if (tag.contains("SelectedTask")) {
            selectedTask = TaskType.valueOf(tag.getString("SelectedTask"))
        }
        if (tag.contains("SelectedBlock")) {
            selectedBlockName = tag.getString("SelectedBlock")
        }
        if (tag.contains("SelectedTool")) {
            selectedToolName = tag.getString("SelectedTool")
        }
        
        // Load building-specific data
        if (tag.contains("BuildMode")) {
            selectedBuildMode = BuildMode.valueOf(tag.getString("BuildMode"))
        }
        if (tag.contains("BuildFromPos")) {
            buildFromPos = BlockPos.of(tag.getLong("BuildFromPos"))
        }
        if (tag.contains("BuildToPos")) {
            buildToPos = BlockPos.of(tag.getLong("BuildToPos"))
        }
        if (tag.contains("SchematicAnchor")) {
            schematicAnchor = BlockPos.of(tag.getLong("SchematicAnchor"))
        }
        if (tag.contains("SchematicName")) {
            schematicName = tag.getString("SchematicName")
        }
    }
    
    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }
    
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registries)
    }
    
    override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        // For now, we'll use a simple screen without a container menu
        // In a full implementation, you'd create a custom ScreenHandler here
        return null
    }
    
    override fun getDisplayName(): Component {
        return Component.translatable("container.programmablegolem.golem_computer")
    }
}
