package com.programmablegolem.blockentity

import com.programmablegolem.ai.BuildMode
import com.programmablegolem.ai.GolemTask
import com.programmablegolem.ai.TaskType
import com.programmablegolem.entity.GolemComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.phys.AABB
import java.util.UUID

class GolemComputerBlockEntity(
    pos: BlockPos,
    state: net.minecraft.world.level.block.state.BlockState
) : BlockEntity(ModBlockEntities.GOLEM_COMPUTER, pos, state) {

    var connectedGolemUUID: UUID? = null
    var isDownloading = false
    var downloadProgress = 0
    var selectedTask: TaskType? = null
    var selectedBlockName: String? = null
    var selectedToolName: String? = null
    var selectedBuildMode: BuildMode? = null
    var buildFromPos: BlockPos? = null
    var buildToPos: BlockPos? = null
    var schematicAnchor: BlockPos? = null
    var schematicName: String? = null

    fun connectGolem(uuid: UUID) {
        connectedGolemUUID = uuid
        setChanged()
    }

    fun startDownload() {
        if (connectedGolemUUID != null && selectedTask != null) {
            isDownloading = true
            downloadProgress = 0
            setChanged()
        }
    }

    fun disconnectGolem() {
        val uuid = connectedGolemUUID
        if (uuid != null && level != null) {
            val box = AABB(blockPos).inflate(20.0)
            val golem = level!!.getEntitiesOfClass(IronGolem::class.java, box).firstOrNull { it.uuid == uuid }
            golem?.let {
                val data = GolemComponent.get(it)
                data.isCabled = false
                it.setNoAi(false)
            }
        }
        
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

    private fun completeDownload(world: Level) {
        isDownloading = false
        downloadProgress = 0
        val uuid = connectedGolemUUID ?: run { setChanged(); return }
        val box = AABB(blockPos).inflate(20.0)
        val golem = world.getEntitiesOfClass(IronGolem::class.java, box).firstOrNull { it.uuid == uuid }
        golem?.let {
            val data = GolemComponent.get(it)
            val task = selectedTask ?: return@let
            data.currentTask = GolemTask(
                task, selectedBlockName, selectedToolName,
                selectedBuildMode, buildFromPos, buildToPos, schematicAnchor, schematicName
            )
            data.isCabled = false
            it.setNoAi(false)
        }
        setChanged()
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        connectedGolemUUID?.let { tag.putUUID("ConnectedGolem", it) }
        tag.putBoolean("IsDownloading", isDownloading)
        tag.putInt("DownloadProgress", downloadProgress)
        selectedTask?.let { tag.putString("SelectedTask", it.name) }
        selectedBlockName?.let { tag.putString("SelectedBlock", it) }
        selectedToolName?.let { tag.putString("SelectedTool", it) }
        selectedBuildMode?.let { tag.putString("SelectedBuildMode", it.name) }
        buildFromPos?.let { tag.putLong("BuildFromPos", it.asLong()) }
        buildToPos?.let { tag.putLong("BuildToPos", it.asLong()) }
        schematicAnchor?.let { tag.putLong("SchematicAnchor", it.asLong()) }
        schematicName?.let { tag.putString("SchematicName", it) }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.hasUUID("ConnectedGolem")) connectedGolemUUID = tag.getUUID("ConnectedGolem")
        isDownloading = tag.getBoolean("IsDownloading")
        downloadProgress = tag.getInt("DownloadProgress")
        if (tag.contains("SelectedTask")) selectedTask = TaskType.valueOf(tag.getString("SelectedTask"))
        if (tag.contains("SelectedBlock")) selectedBlockName = tag.getString("SelectedBlock")
        if (tag.contains("SelectedTool")) selectedToolName = tag.getString("SelectedTool")
        if (tag.contains("SelectedBuildMode")) selectedBuildMode = BuildMode.valueOf(tag.getString("SelectedBuildMode"))
        if (tag.contains("BuildFromPos")) buildFromPos = BlockPos.of(tag.getLong("BuildFromPos"))
        if (tag.contains("BuildToPos")) buildToPos = BlockPos.of(tag.getLong("BuildToPos"))
        if (tag.contains("SchematicAnchor")) schematicAnchor = BlockPos.of(tag.getLong("SchematicAnchor"))
        if (tag.contains("SchematicName")) schematicName = tag.getString("SchematicName")
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = saveWithoutMetadata(registries)
    override fun getUpdatePacket() = net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this)
    
    companion object {
        fun tick(level: Level, pos: BlockPos, state: net.minecraft.world.level.block.state.BlockState, blockEntity: GolemComputerBlockEntity) {
            if (blockEntity.isDownloading) {
                blockEntity.downloadProgress++
                if (blockEntity.downloadProgress >= 60) {
                    blockEntity.completeDownload(level)
                }
                blockEntity.setChanged()
            }
        }
    }
}
