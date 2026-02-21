package com.programmablegolem.gui

import com.programmablegolem.ai.BuildMode
import com.programmablegolem.ai.TaskType
import com.programmablegolem.network.DisconnectCablePayload
import com.programmablegolem.network.ProgramGolemPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import java.util.*

class GolemComputerScreen(
    private val blockPos: BlockPos,
    private val golemUUID: UUID?,
    private val isDownloading: Boolean,
    private val downloadProgress: Int
) : Screen(Component.literal("Golem Computer")) {

    private var selectedTask: TaskType? = null
    private var selectedBlock: String? = null
    private var selectedTool: String? = null
    private var selectedBuildMode: BuildMode? = null
    private var selectedSchematic: String? = null
    
    private var fromXBox: EditBox? = null
    private var fromYBox: EditBox? = null
    private var fromZBox: EditBox? = null
    private var toXBox: EditBox? = null
    private var toYBox: EditBox? = null
    private var toZBox: EditBox? = null
    private var anchorXBox: EditBox? = null
    private var anchorYBox: EditBox? = null
    private var anchorZBox: EditBox? = null
    
    private val taskButtons = mutableListOf<Button>()
    private val optionButtons = mutableListOf<Button>()
    private var downloadButton: Button? = null
    private var disconnectButton: Button? = null
    
    private val MATRIX_GREEN = 0xFF00FF00.toInt()
    private val DARK_BG = 0xC0000000.toInt()
    
    override fun init() {
        super.init()
        
        val centerX = width / 2
        val startY = 40
        
        taskButtons.clear()
        val taskTypes = listOf(
            TaskType.MINING to "⛏ Mining",
            TaskType.FIGHTING to "⚔ Fighting", 
            TaskType.TRADING to "💰 Trading",
            TaskType.BUILDING to "🏗 Building"
        )
        
        taskTypes.forEachIndexed { index, (task, label) ->
            val button = Button.builder(Component.literal(label)) {
                selectedTask = task
                selectedBlock = null
                selectedTool = null
                selectedBuildMode = null
                selectedSchematic = null
                rebuildOptions()
            }
            .bounds(centerX - 200 + (index * 100), startY, 95, 20)
            .build()
            addRenderableWidget(button)
            taskButtons.add(button)
        }
        
        disconnectButton = Button.builder(Component.literal("❌ Disconnect Cable")) {
            ClientPlayNetworking.send(DisconnectCablePayload(blockPos))
            onClose()
        }
        .bounds(width - 160, height - 30, 150, 20)
        .build()
        addRenderableWidget(disconnectButton!!)
        
        rebuildOptions()
    }
    
    private fun rebuildOptions() {
        optionButtons.forEach { removeWidget(it) }
        optionButtons.clear()
        
        listOf(fromXBox, fromYBox, fromZBox, toXBox, toYBox, toZBox, anchorXBox, anchorYBox, anchorZBox)
            .forEach { it?.let { box -> removeWidget(box) } }
        
        downloadButton?.let { removeWidget(it) }
        downloadButton = null
        
        val centerX = width / 2
        val optionsY = 80
        
        when (selectedTask) {
            TaskType.MINING -> {
                val blockBtn = Button.builder(Component.literal("Choose Block: ${selectedBlock ?: "None"}")) {
                    selectedBlock = "minecraft:stone"
                    rebuildOptions()
                }
                .bounds(centerX - 100, optionsY, 200, 20)
                .build()
                addRenderableWidget(blockBtn)
                optionButtons.add(blockBtn)
                
                val toolBtn = Button.builder(Component.literal("Choose Tool: ${selectedTool ?: "None"}")) {
                    selectedTool = "minecraft:iron_pickaxe"
                    rebuildOptions()
                }
                .bounds(centerX - 100, optionsY + 30, 200, 20)
                .build()
                addRenderableWidget(toolBtn)
                optionButtons.add(toolBtn)
                
                if (selectedBlock != null && selectedTool != null) {
                    createDownloadButton()
                }
            }
            
            TaskType.FIGHTING, TaskType.TRADING -> {
                createDownloadButton()
            }
            
            TaskType.BUILDING -> {
                val coordBtn = Button.builder(Component.literal(if (selectedBuildMode == BuildMode.COORDINATES) "▶ Coordinates" else "Coordinates")) {
                    selectedBuildMode = BuildMode.COORDINATES
                    rebuildOptions()
                }
                .bounds(centerX - 100, optionsY, 95, 20)
                .build()
                addRenderableWidget(coordBtn)
                optionButtons.add(coordBtn)
                
                val schematicBtn = Button.builder(Component.literal(if (selectedBuildMode == BuildMode.SCHEMATIC) "▶ Schematic" else "Schematic")) {
                    selectedBuildMode = BuildMode.SCHEMATIC
                    rebuildOptions()
                }
                .bounds(centerX + 5, optionsY, 95, 20)
                .build()
                addRenderableWidget(schematicBtn)
                optionButtons.add(schematicBtn)
                
                if (selectedBuildMode == BuildMode.COORDINATES) {
                    val inputY = optionsY + 40
                    
                    fromXBox = createNumberBox(centerX - 60, inputY, 40)
                    fromYBox = createNumberBox(centerX - 15, inputY, 40)
                    fromZBox = createNumberBox(centerX + 30, inputY, 40)
                    
                    toXBox = createNumberBox(centerX - 60, inputY + 30, 40)
                    toYBox = createNumberBox(centerX - 15, inputY + 30, 40)
                    toZBox = createNumberBox(centerX + 30, inputY + 30, 40)
                    
                    if (fromXBox!!.value.isNotEmpty() && toXBox!!.value.isNotEmpty()) {
                        createDownloadButton()
                    }
                }
                
                if (selectedBuildMode == BuildMode.SCHEMATIC) {
                    val inputY = optionsY + 40
                    
                    anchorXBox = createNumberBox(centerX - 60, inputY, 40)
                    anchorYBox = createNumberBox(centerX - 15, inputY, 40)
                    anchorZBox = createNumberBox(centerX + 30, inputY, 40)
                    
                    val schematics = listOf("Kelp Farm", "Wheat Farm", "Small House", "Castle Tower", "Bridge", "Wall")
                    val schematicBtn = Button.builder(Component.literal("Schematic: ${selectedSchematic ?: "Choose..."}")) {
                        selectedSchematic = schematics.random()
                        rebuildOptions()
                    }
                    .bounds(centerX - 100, inputY + 30, 200, 20)
                    .build()
                    addRenderableWidget(schematicBtn)
                    optionButtons.add(schematicBtn)
                    
                    if (anchorXBox!!.value.isNotEmpty() && selectedSchematic != null) {
                        createDownloadButton()
                    }
                }
            }
            
            TaskType.IDLE -> {}
            null -> {}
        }
    }
    
    private fun createNumberBox(x: Int, y: Int, width: Int): EditBox {
        val box = EditBox(font, x, y, width, 20, Component.empty())
        box.setFilter { s -> s.isEmpty() || s.toIntOrNull() != null || s == "-" }
        addRenderableWidget(box)
        return box
    }
    
    private fun createDownloadButton() {
        downloadButton = Button.builder(Component.literal("▼ Download Program")) {
            sendProgramToServer()
        }
        .bounds(width / 2 - 75, height - 60, 150, 20)
        .build()
        addRenderableWidget(downloadButton!!)
    }
    
    private fun sendProgramToServer() {
        val task = selectedTask ?: return
        
        val fromPos = if (selectedBuildMode == BuildMode.COORDINATES) {
            val x = fromXBox?.value?.toIntOrNull() ?: return
            val y = fromYBox?.value?.toIntOrNull() ?: return
            val z = fromZBox?.value?.toIntOrNull() ?: return
            BlockPos(x, y, z)
        } else null
        
        val toPos = if (selectedBuildMode == BuildMode.COORDINATES) {
            val x = toXBox?.value?.toIntOrNull() ?: return
            val y = toYBox?.value?.toIntOrNull() ?: return
            val z = toZBox?.value?.toIntOrNull() ?: return
            BlockPos(x, y, z)
        } else null
        
        val anchorPos = if (selectedBuildMode == BuildMode.SCHEMATIC) {
            val x = anchorXBox?.value?.toIntOrNull() ?: return
            val y = anchorYBox?.value?.toIntOrNull() ?: return
            val z = anchorZBox?.value?.toIntOrNull() ?: return
            BlockPos(x, y, z)
        } else null
        
        val payload = ProgramGolemPayload(
            blockPos, task, selectedBlock, selectedTool,
            selectedBuildMode, fromPos, toPos, anchorPos, selectedSchematic
        )
        
        ClientPlayNetworking.send(payload)
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, DARK_BG)
        
        if (isDownloading) {
            renderMatrixBackground(graphics, downloadProgress)
        }
        
        graphics.drawCenteredString(font, "GOLEM COMPUTER", width / 2, 15, MATRIX_GREEN)
        
        if (golemUUID != null) {
            graphics.drawCenteredString(font, "✓ Golem Connected", width / 2, height - 50, 0xFF00FF00.toInt())
        } else {
            graphics.drawCenteredString(font, "✗ No Golem Connected", width / 2, height - 50, 0xFFFF0000.toInt())
        }
        
        if (isDownloading) {
            val percent = (downloadProgress / 60f * 100).toInt()
            graphics.drawCenteredString(font, "Downloading... $percent%", width / 2, height / 2, MATRIX_GREEN)
        }
        
        super.render(graphics, mouseX, mouseY, delta)
    }
    
    private fun renderMatrixBackground(graphics: GuiGraphics, progress: Int) {
        val random = kotlin.random.Random(progress)
        for (i in 0 until 50) {
            val x = random.nextInt(width)
            val y = random.nextInt(height)
            val char = ('0'..'1').random()
            graphics.drawString(font, char.toString(), x, y, MATRIX_GREEN)
        }
    }
    
    override fun isPauseScreen() = false
}
