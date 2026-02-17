package com.programmablegolem.ai

import com.programmablegolem.entity.GolemComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.level.block.Blocks
import java.util.EnumSet

class GolemBuildingGoal(private val golem: IronGolem) : Goal() {
    private var currentBuildPos: BlockPos? = null
    private var currentBlockType: String? = null
    private var buildingTicks = 0
    private val BUILDING_TIME = 20 // 1 second per block
    
    // For coordinate mode
    private var buildQueue: MutableList<BlockPos> = mutableListOf()
    private var queueGenerated = false
    
    // For schematic mode
    private var schematicBlocks: MutableList<Pair<BlockPos, String>> = mutableListOf()
    private var schematicLoaded = false
    
    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }
    
    override fun canUse(): Boolean {
        val component = GolemComponent.get(golem)
        if (component.currentTask?.type != TaskType.BUILDING) return false
        
        val task = component.currentTask ?: return false
        
        when (task.buildMode) {
            BuildMode.COORDINATES -> {
                if (!queueGenerated) {
                    generateCoordinateBuildQueue(task)
                }
                return buildQueue.isNotEmpty()
            }
            BuildMode.SCHEMATIC -> {
                if (!schematicLoaded) {
                    loadSchematic(task)
                }
                return schematicBlocks.isNotEmpty()
            }
            else -> return false
        }
    }
    
    override fun canContinueToUse(): Boolean {
        val component = GolemComponent.get(golem)
        val task = component.currentTask ?: return false
        
        return when (task.buildMode) {
            BuildMode.COORDINATES -> buildQueue.isNotEmpty() || currentBuildPos != null
            BuildMode.SCHEMATIC -> schematicBlocks.isNotEmpty() || currentBuildPos != null
            else -> false
        }
    }
    
    override fun start() {
        buildingTicks = 0
        selectNextBuildPosition()
    }
    
    override fun tick() {
        currentBuildPos?.let { pos ->
            // Look at the position
            golem.lookControl.setLookAt(
                pos.x.toDouble() + 0.5,
                pos.y.toDouble() + 0.5,
                pos.z.toDouble() + 0.5
            )
            
            // Move to the position if too far
            val distance = golem.blockPosition().distSqr(pos)
            if (distance > 9.0) { // 3 blocks
                golem.navigation.moveTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), 1.0)
            } else {
                golem.navigation.stop()
                
                // Place block
                buildingTicks++
                if (buildingTicks >= BUILDING_TIME) {
                    placeBlock(pos)
                    buildingTicks = 0
                    currentBuildPos = null
                    selectNextBuildPosition()
                }
            }
        }
    }
    
    override fun stop() {
        currentBuildPos = null
        buildingTicks = 0
        buildQueue.clear()
        schematicBlocks.clear()
        queueGenerated = false
        schematicLoaded = false
    }
    
    private fun selectNextBuildPosition() {
        val component = GolemComponent.get(golem)
        val task = component.currentTask ?: return
        
        when (task.buildMode) {
            BuildMode.COORDINATES -> {
                if (buildQueue.isNotEmpty()) {
                    currentBuildPos = buildQueue.removeAt(0)
                    currentBlockType = task.targetBlockName
                }
            }
            BuildMode.SCHEMATIC -> {
                if (schematicBlocks.isNotEmpty()) {
                    val (pos, blockType) = schematicBlocks.removeAt(0)
                    currentBuildPos = pos
                    currentBlockType = blockType
                }
            }
            else -> {}
        }
    }
    
    private fun generateCoordinateBuildQueue(task: GolemTask) {
        val fromPos = task.buildFromPos ?: return
        val toPos = task.buildToPos ?: return
        
        buildQueue.clear()
        
        val minX = minOf(fromPos.x, toPos.x)
        val maxX = maxOf(fromPos.x, toPos.x)
        val minY = minOf(fromPos.y, toPos.y)
        val maxY = maxOf(fromPos.y, toPos.y)
        val minZ = minOf(fromPos.z, toPos.z)
        val maxZ = maxOf(fromPos.z, toPos.z)
        
        // Generate all positions in the area
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val pos = BlockPos(x, y, z)
                    // Only add if current block is air and there's support below
                    if (golem.level().getBlockState(pos).isAir) {
                        buildQueue.add(pos)
                    }
                }
            }
        }
        
        queueGenerated = true
    }
    
    private fun loadSchematic(task: GolemTask) {
        val schematicName = task.schematicName ?: return
        val anchor = task.schematicAnchor ?: golem.blockPosition()
        
        // TODO: Implement actual schematic loading from .schem or .schematic files
        // For now, this creates a simple structure as an example
        // In a full implementation, you would:
        // 1. Load .schem or .schematic file from world/schematics/ folder
        // 2. Parse the NBT structure
        // 3. Extract block positions and types relative to origin
        // 4. Add anchor offset to each position
        
        schematicBlocks.clear()
        
        // Example: Create a simple structure based on schematic name
        when (schematicName) {
            "kelp_farm" -> {
                // Create a 5x5 water basin with kelp
                for (x in 0..4) {
                    for (z in 0..4) {
                        // Water blocks
                        schematicBlocks.add(Pair(
                            anchor.offset(x, 0, z),
                            "minecraft:water"
                        ))
                        // Kelp plants
                        if (x % 2 == 0 && z % 2 == 0) {
                            schematicBlocks.add(Pair(
                                anchor.offset(x, 1, z),
                                "minecraft:kelp_plant"
                            ))
                        }
                    }
                }
            }
            "wheat_farm" -> {
                // Create a 9x9 farmland with water in center
                for (x in 0..8) {
                    for (z in 0..8) {
                        if (x == 4 && z == 4) {
                            schematicBlocks.add(Pair(
                                anchor.offset(x, 0, z),
                                "minecraft:water"
                            ))
                        } else {
                            schematicBlocks.add(Pair(
                                anchor.offset(x, 0, z),
                                "minecraft:farmland"
                            ))
                        }
                    }
                }
            }
            "house_small" -> {
                // Create walls for a small house (5x5x3)
                for (y in 0..2) {
                    for (x in 0..4) {
                        for (z in 0..4) {
                            // Only walls (hollow inside)
                            if (x == 0 || x == 4 || z == 0 || z == 4) {
                                val blockType = task.targetBlockName ?: "minecraft:oak_planks"
                                schematicBlocks.add(Pair(anchor.offset(x, y, z), blockType))
                            }
                        }
                    }
                }
            }
            else -> {
                // Default: Create a small 3x3x3 cube
                for (y in 0..2) {
                    for (x in 0..2) {
                        for (z in 0..2) {
                            val blockType = task.targetBlockName ?: "minecraft:stone"
                            schematicBlocks.add(Pair(anchor.offset(x, y, z), blockType))
                        }
                    }
                }
            }
        }
        
        schematicLoaded = true
    }
    
    private fun placeBlock(pos: BlockPos) {
        currentBlockType?.let { blockName ->
            val block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName))
            if (block != Blocks.AIR && golem.level().getBlockState(pos).isAir) {
                golem.level().setBlock(pos, block.defaultBlockState(), 3)
            }
        }
    }
}
