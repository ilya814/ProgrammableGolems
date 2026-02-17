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

class GolemComputerScreen(
    private val computerPos: BlockPos,
    private val isConnected: Boolean,
    private val isDownloading: Boolean,
    private val downloadProgress: Int
) : Screen(Component.literal("Golem Computer")) {

    private var selectedTask: TaskType? = null
    private var selectedBlock: String? = null
    private var selectedTool: String? = null
    private var selectedBuildMode: BuildMode? = null

    private var fromX: EditBox? = null; private var fromY: EditBox? = null; private var fromZ: EditBox? = null
    private var toX: EditBox? = null;   private var toY: EditBox? = null;   private var toZ: EditBox? = null
    private var anchorX: EditBox? = null; private var anchorY: EditBox? = null; private var anchorZ: EditBox? = null

    private var selectedSchematic: String? = null
    private val schematics = listOf("kelp_farm", "wheat_farm", "house_small", "castle_tower", "bridge", "wall_section")

    override fun init() {
        super.init()
        val W = 200; val H = 20; val cx = width / 2; var y = 50

        if (!isConnected) {
            addRenderableWidget(Button.builder(Component.literal("No Golem Connected")) {}.bounds(cx - W/2, y, W, H).build())
            return
        }
        if (isDownloading) return

        for (task in listOf(TaskType.MINING, TaskType.FIGHTING, TaskType.TRADING, TaskType.BUILDING)) {
            val t = task
            addRenderableWidget(Button.builder(Component.literal(
                if (selectedTask == t) "> ${t.displayName} <" else t.displayName
            )) { selectedTask = t; selectedBuildMode = null; clearWidgets(); init() }.bounds(cx - W/2, y, W, H).build())
            y += 24
        }
        y += 10

        selectedTask?.let { task ->
            when (task) {
                TaskType.MINING -> {
                    addRenderableWidget(Button.builder(Component.literal("Block: ${selectedBlock ?: "minecraft:stone"}")) {
                        selectedBlock = when (selectedBlock) {
                            null, "minecraft:coal_ore" -> "minecraft:stone"
                            "minecraft:stone" -> "minecraft:iron_ore"
                            "minecraft:iron_ore" -> "minecraft:diamond_ore"
                            else -> "minecraft:coal_ore"
                        }; clearWidgets(); init()
                    }.bounds(cx - W/2, y, W, H).build()); y += 24
                    addRenderableWidget(Button.builder(Component.literal("Tool: ${selectedTool ?: "pickaxe"}")) {
                        selectedTool = when (selectedTool) { null, "axe" -> "pickaxe"; "pickaxe" -> "shovel"; else -> "axe" }
                        clearWidgets(); init()
                    }.bounds(cx - W/2, y, W, H).build()); y += 24
                }
                TaskType.BUILDING -> {
                    addRenderableWidget(Button.builder(Component.literal("Block: ${selectedBlock ?: "minecraft:cobblestone"}")) {
                        selectedBlock = when (selectedBlock) {
                            null, "minecraft:glass" -> "minecraft:cobblestone"
                            "minecraft:cobblestone" -> "minecraft:stone_bricks"
                            "minecraft:stone_bricks" -> "minecraft:oak_planks"
                            else -> "minecraft:glass"
                        }; clearWidgets(); init()
                    }.bounds(cx - W/2, y, W, H).build()); y += 30

                    addRenderableWidget(Button.builder(Component.literal(
                        if (selectedBuildMode == BuildMode.COORDINATES) "> Coordinates <" else "Coordinates"
                    )) { selectedBuildMode = BuildMode.COORDINATES; clearWidgets(); init() }.bounds(cx - W/2, y, W/2 - 2, H).build())
                    addRenderableWidget(Button.builder(Component.literal(
                        if (selectedBuildMode == BuildMode.SCHEMATIC) "> Schematic <" else "Schematic"
                    )) { selectedBuildMode = BuildMode.SCHEMATIC; clearWidgets(); init() }.bounds(cx + 2, y, W/2 - 2, H).build())
                    y += 30

                    when (selectedBuildMode) {
                        BuildMode.COORDINATES -> {
                            val iw = 60; val sp = 4; val sx = cx - (iw * 3 + sp * 2) / 2
                            fromX = EditBox(font, sx, y, iw, H, Component.literal("X")).also { it.value = "0"; addRenderableWidget(it) }
                            fromY = EditBox(font, sx+iw+sp, y, iw, H, Component.literal("Y")).also { it.value = "64"; addRenderableWidget(it) }
                            fromZ = EditBox(font, sx+(iw+sp)*2, y, iw, H, Component.literal("Z")).also { it.value = "0"; addRenderableWidget(it) }
                            y += 30
                            toX = EditBox(font, sx, y, iw, H, Component.literal("X")).also { it.value = "10"; addRenderableWidget(it) }
                            toY = EditBox(font, sx+iw+sp, y, iw, H, Component.literal("Y")).also { it.value = "64"; addRenderableWidget(it) }
                            toZ = EditBox(font, sx+(iw+sp)*2, y, iw, H, Component.literal("Z")).also { it.value = "10"; addRenderableWidget(it) }
                            y += 30
                        }
                        BuildMode.SCHEMATIC -> {
                            val iw = 60; val sp = 4; val sx = cx - (iw * 3 + sp * 2) / 2
                            anchorX = EditBox(font, sx, y, iw, H, Component.literal("X")).also { it.value = "0"; addRenderableWidget(it) }
                            anchorY = EditBox(font, sx+iw+sp, y, iw, H, Component.literal("Y")).also { it.value = "64"; addRenderableWidget(it) }
                            anchorZ = EditBox(font, sx+(iw+sp)*2, y, iw, H, Component.literal("Z")).also { it.value = "0"; addRenderableWidget(it) }
                            y += 30
                            addRenderableWidget(Button.builder(Component.literal("Schematic: ${selectedSchematic ?: "click to pick"}")) {
                                val i = schematics.indexOf(selectedSchematic ?: "")
                                selectedSchematic = schematics[if (i < 0) 0 else (i + 1) % schematics.size]
                                clearWidgets(); init()
                            }.bounds(cx - W/2, y, W, H).build()); y += 30
                        }
                        else -> {}
                    }
                }
                else -> {}
            }

            if (canDownload()) {
                y += 6
                addRenderableWidget(Button.builder(Component.literal("▼ Download Program")) {
                    sendProgram(); onClose()
                }.bounds(cx - W/2, y, W, H).build())
            }
        }

        addRenderableWidget(Button.builder(Component.literal("Disconnect Cable")) {
            ClientPlayNetworking.send(DisconnectCablePayload(computerPos)); onClose()
        }.bounds(cx - W/2, height - 30, W, H).build())
    }

    private fun canDownload() = when (selectedTask) {
        TaskType.MINING -> selectedBlock != null && selectedTool != null
        TaskType.FIGHTING, TaskType.TRADING -> true
        TaskType.BUILDING -> selectedBlock != null && selectedBuildMode != null && when (selectedBuildMode) {
            BuildMode.COORDINATES -> listOf(fromX, fromY, fromZ, toX, toY, toZ).all { it?.value?.toIntOrNull() != null }
            BuildMode.SCHEMATIC -> anchorX?.value?.toIntOrNull() != null && selectedSchematic != null
            else -> false
        }
        else -> false
    }

    private fun sendProgram() {
        val task = selectedTask ?: return
        val fromPos = if (selectedBuildMode == BuildMode.COORDINATES)
            BlockPos(fromX!!.value.toInt(), fromY!!.value.toInt(), fromZ!!.value.toInt()) else null
        val toPos = if (selectedBuildMode == BuildMode.COORDINATES)
            BlockPos(toX!!.value.toInt(), toY!!.value.toInt(), toZ!!.value.toInt()) else null
        val anchor = if (selectedBuildMode == BuildMode.SCHEMATIC)
            BlockPos(anchorX!!.value.toInt(), anchorY!!.value.toInt(), anchorZ!!.value.toInt()) else null
        ClientPlayNetworking.send(ProgramGolemPayload(
            computerPos, task, selectedBlock, selectedTool,
            selectedBuildMode, fromPos, toPos, anchor, selectedSchematic
        ))
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, delta: Float) {
        renderBackground(g, mx, my, delta)
        super.render(g, mx, my, delta)
        g.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF)
        if (isDownloading) g.drawCenteredString(font, Component.literal("Downloading... ${downloadProgress * 100 / 60}%"), width / 2, height / 2, 0x00FF00)
        selectedTask?.let { g.drawCenteredString(font, Component.literal("Task: ${it.displayName}"), width / 2, 38, 0xFFFF00) }
        if (selectedBuildMode == BuildMode.COORDINATES) {
            fromX?.let { g.drawString(font, "From:", it.x - 38, it.y + 5, 0xFFFFFF) }
            toX?.let { g.drawString(font, "To:", it.x - 38, it.y + 5, 0xFFFFFF) }
        }
        if (selectedBuildMode == BuildMode.SCHEMATIC) {
            anchorX?.let { g.drawString(font, "Anchor:", it.x - 48, it.y + 5, 0xFFFFFF) }
        }
    }

    override fun isPauseScreen() = false
}
