package com.programmablegolem.gui

import com.programmablegolem.ai.BuildMode
import com.programmablegolem.ai.TaskType
import com.programmablegolem.network.ModNetworking
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component

class GolemComputerScreen(
    private val computerPos: BlockPos,
    private val isConnected: Boolean,
    private val isDownloading: Boolean,
    private val downloadProgress: Int
) : Screen(Component.literal("Golem Computer")) {
    
    private var selectedTask: TaskType? = null
    private var selectedBlock: String? = null
    private var selectedTool: String? = null
    
    // Building mode fields
    private var selectedBuildMode: BuildMode? = null
    private var buildFromX: EditBox? = null
    private var buildFromY: EditBox? = null
    private var buildFromZ: EditBox? = null
    private var buildToX: EditBox? = null
    private var buildToY: EditBox? = null
    private var buildToZ: EditBox? = null
    
    // Schematic mode fields
    private var schematicAnchorX: EditBox? = null
    private var schematicAnchorY: EditBox? = null
    private var schematicAnchorZ: EditBox? = null
    private var selectedSchematic: String? = null
    private var availableSchematics = listOf(
        "kelp_farm",
        "wheat_farm", 
        "house_small",
        "castle_tower",
        "bridge",
        "wall_section",
        "custom_build"
    )
    
    override fun init() {
        super.init()
        
        val buttonWidth = 200
        val buttonHeight = 20
        val centerX = width / 2
        var yPos = 60
        
        if (!isConnected) {
            addRenderableWidget(
                Button.builder(Component.literal("No Golem Connected")) { }
                    .bounds(centerX - buttonWidth / 2, yPos, buttonWidth, buttonHeight)
                    .build()
            )
            return
        }
        
        if (isDownloading) {
            // Show download progress
            return
        }
        
        // Task selection buttons
        addRenderableWidget(
            Button.builder(Component.literal("Mining")) { selectTask(TaskType.MINING) }
                .bounds(centerX - buttonWidth / 2, yPos, buttonWidth, buttonHeight)
                .build()
        )
        yPos += 25
        
        addRenderableWidget(
            Button.builder(Component.literal("Fighting")) { selectTask(TaskType.FIGHTING) }
                .bounds(centerX - buttonWidth / 2, yPos, buttonWidth, buttonHeight)
                .build()
        )
        yPos += 25
        
        addRenderableWidget(
            Button.builder(Component.literal("Trading")) { selectTask(TaskType.TRADING) }
                .bounds(centerX - buttonWidth / 2, yPos, buttonWidth, buttonHeight)
                .build()
        )
        yPos += 25
        
        addRenderableWidget(
            Button.builder(Component.literal("Building")) { selectTask(TaskType.BUILDING) }
                .bounds(centerX - buttonWidth / 2, yPos, buttonWidth, buttonHeight)
                .build()
        )
        yPos += 40
        
        // If task is selected, show task-specific options
        selectedTask?.let { task ->
            when (task) {
                TaskType.MINING -> {
                    yPos = setupMiningOptions(centerX, yPos, buttonWidth, buttonHeight)
                }
                TaskType.BUILDING -> {
                    yPos = setupBuildingOptions(centerX, yPos, buttonWidth, buttonHeight)
                }
                else -> {}
            }
            
            yPos += 10
            
            // Download button (only if configuration is complete)
            if (isConfigurationComplete()) {
                addRenderableWidget(
                    Button.builder(Component.literal("Download Program")) {
                        sendProgramPacket()
                    }
                        .bounds(centerX - buttonWidth / 2, yPos, buttonWidth, buttonHeight)
                        .build()
                )
            }
        }
        
        yPos += 40
        
        // Disconnect button
        addRenderableWidget(
            Button.builder(Component.literal("Disconnect Cable")) {
                sendDisconnectPacket()
                onClose()
            }
                .bounds(centerX - buttonWidth / 2, yPos, buttonWidth, buttonHeight)
                .build()
        )
    }
    
    private fun setupMiningOptions(centerX: Int, yPos: Int, buttonWidth: Int, buttonHeight: Int): Int {
        var y = yPos
        
        addRenderableWidget(
            Button.builder(Component.literal("Select Block: ${selectedBlock ?: "None"}")) {
                // Cycle through common blocks
                selectedBlock = when (selectedBlock) {
                    null -> "minecraft:stone"
                    "minecraft:stone" -> "minecraft:dirt"
                    "minecraft:dirt" -> "minecraft:oak_log"
                    "minecraft:oak_log" -> "minecraft:iron_ore"
                    else -> "minecraft:stone"
                }
                clearWidgets()
                init()
            }
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                .build()
        )
        y += 25
        
        addRenderableWidget(
            Button.builder(Component.literal("Select Tool: ${selectedTool ?: "None"}")) {
                // Cycle through tools
                selectedTool = when (selectedTool) {
                    null -> "pickaxe"
                    "pickaxe" -> "shovel"
                    "shovel" -> "axe"
                    else -> "pickaxe"
                }
                clearWidgets()
                init()
            }
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                .build()
        )
        y += 25
        
        return y
    }
    
    private fun setupBuildingOptions(centerX: Int, yPos: Int, buttonWidth: Int, buttonHeight: Int): Int {
        var y = yPos
        
        // Block selection
        addRenderableWidget(
            Button.builder(Component.literal("Select Block: ${selectedBlock ?: "None"}")) {
                selectedBlock = when (selectedBlock) {
                    null -> "minecraft:cobblestone"
                    "minecraft:cobblestone" -> "minecraft:stone_bricks"
                    "minecraft:stone_bricks" -> "minecraft:oak_planks"
                    "minecraft:oak_planks" -> "minecraft:glass"
                    else -> "minecraft:cobblestone"
                }
                clearWidgets()
                init()
            }
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                .build()
        )
        y += 30
        
        // Mode selection buttons
        addRenderableWidget(
            Button.builder(Component.literal("Mode: Coordinates")) {
                selectedBuildMode = BuildMode.COORDINATES
                clearWidgets()
                init()
            }
                .bounds(centerX - buttonWidth / 2, y, buttonWidth / 2 - 5, buttonHeight)
                .build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("Mode: Schematic")) {
                selectedBuildMode = BuildMode.SCHEMATIC
                clearWidgets()
                init()
            }
                .bounds(centerX + 5, y, buttonWidth / 2 - 5, buttonHeight)
                .build()
        )
        y += 30
        
        // Show mode-specific options
        selectedBuildMode?.let { mode ->
            when (mode) {
                BuildMode.COORDINATES -> {
                    y = setupCoordinateInputs(centerX, y, buttonWidth, buttonHeight)
                }
                BuildMode.SCHEMATIC -> {
                    y = setupSchematicInput(centerX, y, buttonWidth, buttonHeight)
                }
            }
        }
        
        return y
    }
    
    private fun setupCoordinateInputs(centerX: Int, yPos: Int, buttonWidth: Int, buttonHeight: Int): Int {
        var y = yPos
        val inputWidth = 50
        val spacing = 5
        
        // FROM coordinates label
        y += 5
        
        // FROM X, Y, Z
        val fromStartX = centerX - (inputWidth * 3 + spacing * 2) / 2
        
        buildFromX = EditBox(font, fromStartX, y, inputWidth, buttonHeight, Component.literal("From X"))
        buildFromY = EditBox(font, fromStartX + inputWidth + spacing, y, inputWidth, buttonHeight, Component.literal("From Y"))
        buildFromZ = EditBox(font, fromStartX + (inputWidth + spacing) * 2, y, inputWidth, buttonHeight, Component.literal("From Z"))
        
        buildFromX?.setMaxLength(6)
        buildFromY?.setMaxLength(6)
        buildFromZ?.setMaxLength(6)
        
        buildFromX?.value = "0"
        buildFromY?.value = "64"
        buildFromZ?.value = "0"
        
        addRenderableWidget(buildFromX!!)
        addRenderableWidget(buildFromY!!)
        addRenderableWidget(buildFromZ!!)
        
        y += 30
        
        // TO coordinates label
        y += 5
        
        // TO X, Y, Z
        buildToX = EditBox(font, fromStartX, y, inputWidth, buttonHeight, Component.literal("To X"))
        buildToY = EditBox(font, fromStartX + inputWidth + spacing, y, inputWidth, buttonHeight, Component.literal("To Y"))
        buildToZ = EditBox(font, fromStartX + (inputWidth + spacing) * 2, y, inputWidth, buttonHeight, Component.literal("To Z"))
        
        buildToX?.setMaxLength(6)
        buildToY?.setMaxLength(6)
        buildToZ?.setMaxLength(6)
        
        buildToX?.value = "10"
        buildToY?.value = "70"
        buildToZ?.value = "10"
        
        addRenderableWidget(buildToX!!)
        addRenderableWidget(buildToY!!)
        addRenderableWidget(buildToZ!!)
        
        y += 30
        
        return y
    }
    
    private fun setupSchematicInput(centerX: Int, yPos: Int, buttonWidth: Int, buttonHeight: Int): Int {
        var y = yPos
        val inputWidth = 50
        val spacing = 5
        
        y += 10
        
        // Anchor coordinate inputs (single coordinate - the starting corner)
        val anchorStartX = centerX - (inputWidth * 3 + spacing * 2) / 2
        
        schematicAnchorX = EditBox(font, anchorStartX, y, inputWidth, buttonHeight, Component.literal("Anchor X"))
        schematicAnchorY = EditBox(font, anchorStartX + inputWidth + spacing, y, inputWidth, buttonHeight, Component.literal("Anchor Y"))
        schematicAnchorZ = EditBox(font, anchorStartX + (inputWidth + spacing) * 2, y, inputWidth, buttonHeight, Component.literal("Anchor Z"))
        
        schematicAnchorX?.setMaxLength(6)
        schematicAnchorY?.setMaxLength(6)
        schematicAnchorZ?.setMaxLength(6)
        
        schematicAnchorX?.value = "0"
        schematicAnchorY?.value = "64"
        schematicAnchorZ?.value = "0"
        
        addRenderableWidget(schematicAnchorX!!)
        addRenderableWidget(schematicAnchorY!!)
        addRenderableWidget(schematicAnchorZ!!)
        
        y += 35
        
        // Browse schematics button
        addRenderableWidget(
            Button.builder(Component.literal("Browse Schematics")) {
                // Cycle through available schematics
                val currentIndex = availableSchematics.indexOf(selectedSchematic ?: "")
                val nextIndex = if (currentIndex >= 0) {
                    (currentIndex + 1) % availableSchematics.size
                } else {
                    0
                }
                selectedSchematic = availableSchematics[nextIndex]
                clearWidgets()
                init()
            }
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                .build()
        )
        
        y += 30
        
        // Show selected schematic
        selectedSchematic?.let {
            // Display selected schematic name (rendered in render method)
        }
        
        return y
    }
    
    private fun isConfigurationComplete(): Boolean {
        return when (selectedTask) {
            TaskType.MINING -> selectedBlock != null && selectedTool != null
            TaskType.FIGHTING, TaskType.TRADING -> true
            TaskType.BUILDING -> {
                selectedBlock != null && selectedBuildMode != null && when (selectedBuildMode) {
                    BuildMode.COORDINATES -> {
                        buildFromX?.value?.toIntOrNull() != null &&
                        buildFromY?.value?.toIntOrNull() != null &&
                        buildFromZ?.value?.toIntOrNull() != null &&
                        buildToX?.value?.toIntOrNull() != null &&
                        buildToY?.value?.toIntOrNull() != null &&
                        buildToZ?.value?.toIntOrNull() != null
                    }
                    BuildMode.SCHEMATIC -> {
                        schematicAnchorX?.value?.toIntOrNull() != null &&
                        schematicAnchorY?.value?.toIntOrNull() != null &&
                        schematicAnchorZ?.value?.toIntOrNull() != null &&
                        selectedSchematic != null
                    }
                    else -> false
                }
            }
            else -> false
        }
    }
    
    private fun selectTask(task: TaskType) {
        selectedTask = task
        // Reset building options when switching tasks
        selectedBuildMode = null
        clearWidgets()
        init()
    }
    
    private fun sendProgramPacket() {
        selectedTask?.let { task ->
            val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(computerPos)
            buf.writeUtf(task.name)
            buf.writeBoolean(selectedBlock != null)
            selectedBlock?.let { buf.writeUtf(it) }
            buf.writeBoolean(selectedTool != null)
            selectedTool?.let { buf.writeUtf(it) }
            
            // Building-specific data
            if (task == TaskType.BUILDING) {
                buf.writeBoolean(selectedBuildMode != null)
                selectedBuildMode?.let { buf.writeUtf(it.name) }
                
                when (selectedBuildMode) {
                    BuildMode.COORDINATES -> {
                        val fromX = buildFromX?.value?.toIntOrNull() ?: 0
                        val fromY = buildFromY?.value?.toIntOrNull() ?: 0
                        val fromZ = buildFromZ?.value?.toIntOrNull() ?: 0
                        val toX = buildToX?.value?.toIntOrNull() ?: 0
                        val toY = buildToY?.value?.toIntOrNull() ?: 0
                        val toZ = buildToZ?.value?.toIntOrNull() ?: 0
                        
                        buf.writeBlockPos(BlockPos(fromX, fromY, fromZ))
                        buf.writeBlockPos(BlockPos(toX, toY, toZ))
                        buf.writeBoolean(false) // No anchor pos
                        buf.writeBoolean(false) // No schematic name
                    }
                    BuildMode.SCHEMATIC -> {
                        val anchorX = schematicAnchorX?.value?.toIntOrNull() ?: 0
                        val anchorY = schematicAnchorY?.value?.toIntOrNull() ?: 0
                        val anchorZ = schematicAnchorZ?.value?.toIntOrNull() ?: 0
                        
                        buf.writeBlockPos(BlockPos.ZERO) // No from pos
                        buf.writeBlockPos(BlockPos.ZERO) // No to pos
                        buf.writeBoolean(true) // Has anchor pos
                        buf.writeBlockPos(BlockPos(anchorX, anchorY, anchorZ))
                        buf.writeBoolean(true) // Has schematic name
                        buf.writeUtf(selectedSchematic ?: "default")
                    }
                    else -> {
                        buf.writeBlockPos(BlockPos.ZERO)
                        buf.writeBlockPos(BlockPos.ZERO)
                        buf.writeBoolean(false)
                        buf.writeBoolean(false)
                    }
                }
            } else {
                buf.writeBoolean(false) // No build mode
            }
            
            ClientPlayNetworking.send(ModNetworking.PROGRAM_GOLEM_PACKET, buf)
            onClose()
        }
    }
    
    private fun sendDisconnectPacket() {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeBlockPos(computerPos)
        
        ClientPlayNetworking.send(ModNetworking.DISCONNECT_CABLE_PACKET, buf)
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(graphics, mouseX, mouseY, delta)
        super.render(graphics, mouseX, mouseY, delta)
        
        // Title
        graphics.drawCenteredString(
            font,
            title,
            width / 2,
            20,
            0xFFFFFF
        )
        
        if (isDownloading) {
            val progressPercent = (downloadProgress * 100 / 60)
            graphics.drawCenteredString(
                font,
                Component.literal("Downloading... $progressPercent%"),
                width / 2,
                height / 2,
                0x00FF00
            )
        }
        
        selectedTask?.let { task ->
            graphics.drawCenteredString(
                font,
                Component.literal("Selected: ${task.displayName}"),
                width / 2,
                40,
                0xFFFF00
            )
        }
        
        // Draw labels for coordinate inputs
        if (selectedTask == TaskType.BUILDING && selectedBuildMode == BuildMode.COORDINATES) {
            buildFromX?.let {
                graphics.drawString(
                    font,
                    "From (X, Y, Z):",
                    width / 2 - 100,
                    it.y - 12,
                    0xFFFFFF
                )
            }
            buildToX?.let {
                graphics.drawString(
                    font,
                    "To (X, Y, Z):",
                    width / 2 - 100,
                    it.y - 12,
                    0xFFFFFF
                )
            }
        }
        
        // Draw labels for schematic mode
        if (selectedTask == TaskType.BUILDING && selectedBuildMode == BuildMode.SCHEMATIC) {
            schematicAnchorX?.let {
                graphics.drawString(
                    font,
                    "Anchor Point (X, Y, Z):",
                    width / 2 - 100,
                    it.y - 12,
                    0xFFFFFF
                )
            }
            
            // Show selected schematic
            selectedSchematic?.let { schematic ->
                graphics.drawCenteredString(
                    font,
                    Component.literal("Selected: $schematic"),
                    width / 2,
                    height / 2 + 30,
                    0x00FF00
                )
            }
        }
    }
    
    override fun isPauseScreen(): Boolean = false
}
