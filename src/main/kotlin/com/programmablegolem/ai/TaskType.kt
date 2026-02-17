package com.programmablegolem.ai

import net.minecraft.core.BlockPos

enum class TaskType(val displayName: String) {
    MINING("Mining"),
    FIGHTING("Fighting"),
    TRADING("Trading"),
    BUILDING("Building"),
    IDLE("Idle")
}

enum class BuildMode {
    COORDINATES,
    SCHEMATIC
}

data class GolemTask(
    val type: TaskType,
    val targetBlockName: String? = null,
    val toolName: String? = null,
    // Building-specific fields
    val buildMode: BuildMode? = null,
    // For COORDINATES mode: two corners of the area
    val buildFromPos: BlockPos? = null,
    val buildToPos: BlockPos? = null,
    // For SCHEMATIC mode: anchor point + schematic file
    val schematicAnchor: BlockPos? = null,
    val schematicName: String? = null
)
