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
    private val downloadTime = 60

    var selectedTask: TaskType? = null
    var selectedBlockName: String? = null
    var selectedToolName: String? = null
    var selectedBuildMode: BuildMode? = null
    var buildFromPos: BlockPos? = null
    var buildToPos: BlockPos? = null
    var schematicAnchor: BlockPos? = null
    var schematicName: String? = null

    companion object {
        fun tick(level: Level, pos: BlockPos, state: BlockState, be: GolemComputerBlockEntity) {
            if (level.isClientSide) return
            if (be.isDownloading) {
                be.downloadProgress++
                if (be.downloadProgress >= be.downloadTime) be.completeDownload(level)
                be.setChanged()
            }
        }
    }

    fun startProgramming(
        taskType: TaskType, blockName: String?, toolName: String?,
        buildMode: BuildMode? = null, fromPos: BlockPos? = null, toPos: BlockPos? = null,
        anchorPos: BlockPos? = null, schematic: String? = null
    ) {
        selectedTask = taskType; selectedBlockName = blockName; selectedToolName = toolName
        selectedBuildMode = buildMode; buildFromPos = fromPos; buildToPos = toPos
        schematicAnchor = anchorPos; schematicName = schematic
        isDownloading = true; downloadProgress = 0; setChanged()
    }

    private fun completeDownload(world: Level) {
    isDownloading = false; downloadProgress = 0
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
        
        // UNFREEZE golem - enable AI
        it.setNoAi(false)
    }
    setChanged()
}
fun disconnectGolem() {
    val uuid = connectedGolemUUID
    if (uuid != null && level != null) {
        val box = AABB(blockPos).inflate(20.0)
        val golem = level!!.getEntitiesOfClass(IronGolem::class.java, box).firstOrNull { it.uuid == uuid }
        golem?.let {
            val data = GolemComponent.get(it)
            data.isCabled = false
            it.setNoAi(false) // UNFREEZE
        }
    }
    
    connectedGolemUUID = null; isDownloading = false; downloadProgress = 0
    selectedTask = null; selectedBlockName = null; selectedToolName = null
    selectedBuildMode = null; buildFromPos = null; buildToPos = null
    schematicAnchor = null; schematicName = null; setChanged()
}

    fun disconnectGolem() {
        connectedGolemUUID = null; isDownloading = false; downloadProgress = 0
        selectedTask = null; selectedBlockName = null; selectedToolName = null
        selectedBuildMode = null; buildFromPos = null; buildToPos = null
        schematicAnchor = null; schematicName = null; setChanged()
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        connectedGolemUUID?.let { tag.putUUID("ConnectedGolem", it) }
        tag.putBoolean("IsDownloading", isDownloading)
        tag.putInt("DownloadProgress", downloadProgress)
        selectedTask?.let { tag.putString("SelectedTask", it.name) }
        selectedBlockName?.let { tag.putString("SelectedBlock", it) }
        selectedToolName?.let { tag.putString("SelectedTool", it) }
        selectedBuildMode?.let { tag.putString("BuildMode", it.name) }
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
        if (tag.contains("BuildMode")) selectedBuildMode = BuildMode.valueOf(tag.getString("BuildMode"))
        if (tag.contains("BuildFromPos")) buildFromPos = BlockPos.of(tag.getLong("BuildFromPos"))
        if (tag.contains("BuildToPos")) buildToPos = BlockPos.of(tag.getLong("BuildToPos"))
        if (tag.contains("SchematicAnchor")) schematicAnchor = BlockPos.of(tag.getLong("SchematicAnchor"))
        if (tag.contains("SchematicName")) schematicName = tag.getString("SchematicName")
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)
    override fun getUpdateTag(r: HolderLookup.Provider) = saveWithoutMetadata(r)
    override fun createMenu(id: Int, inv: Inventory, p: Player): AbstractContainerMenu? = null
    override fun getDisplayName() = Component.literal("Golem Computer")
}
