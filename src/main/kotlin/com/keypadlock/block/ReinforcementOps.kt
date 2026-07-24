package com.keypadlock.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

/**
 * Logica compartida para revertir un bloque reforzado a su version vanilla
 * (KeyItem la usa cuando el dueño usa la llave, NetworkHandlersServer la usa
 * cuando se cancela la creacion de password antes de confirmarla).
 */
object ReinforcementOps {

    fun revertChest(level: Level, pos: BlockPos) {
        val state = level.getBlockState(pos)
        if (state.block !is ReinforcedChestBlock) return
        val isDouble = state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
        val otherPos = if (isDouble) ChestBlock.getConnectedBlockPos(pos, state) else null
        val otherState = otherPos?.let { level.getBlockState(it) }

        revertChestHalf(level, pos, state)
        if (otherPos != null && otherState != null && otherState.block is ReinforcedChestBlock) {
            revertChestHalf(level, otherPos, otherState)
        }
    }

    private fun revertChestHalf(level: Level, pos: BlockPos, state: BlockState) {
        val be = level.getBlockEntity(pos) as? ReinforcedChestBlockEntity ?: return
        val items = (0 until be.containerSize).map { be.getItem(it).copy() }
        // Ver LockItem.convertChestHalf: hay que vaciar el block entity viejo ANTES
        // de reemplazar el bloque, o vanilla dropea su contenido solo
        // (BlockEntity.preRemoveSideEffects) y quedan duplicados.
        be.clearContent()

        val newState = Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.FACING, state.getValue(ChestBlock.FACING))
            .setValue(ChestBlock.TYPE, state.getValue(ChestBlock.TYPE))
            .setValue(ChestBlock.WATERLOGGED, state.getValue(ChestBlock.WATERLOGGED))
        level.setBlockAndUpdate(pos, newState)

        val newBe = level.getBlockEntity(pos) as? ChestBlockEntity
        if (newBe != null) {
            items.forEachIndexed { i, stack -> newBe.setItem(i, stack) }
            newBe.setChanged()
        }
    }

    fun revertDoor(level: Level, lowerPos: BlockPos) {
        val lowerState = level.getBlockState(lowerPos)
        if (lowerState.block !is ReinforcedIronDoorBlock) return
        val upperPos = lowerPos.above()
        val upperState = level.getBlockState(upperPos)

        val newLower = Blocks.IRON_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, lowerState.getValue(DoorBlock.FACING))
            .setValue(DoorBlock.HINGE, lowerState.getValue(DoorBlock.HINGE))
            .setValue(DoorBlock.OPEN, lowerState.getValue(DoorBlock.OPEN))
            .setValue(DoorBlock.POWERED, lowerState.getValue(DoorBlock.POWERED))
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
        val newUpper = Blocks.IRON_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, lowerState.getValue(DoorBlock.FACING))
            .setValue(DoorBlock.HINGE, upperState.getValue(DoorBlock.HINGE))
            .setValue(DoorBlock.OPEN, lowerState.getValue(DoorBlock.OPEN))
            .setValue(DoorBlock.POWERED, lowerState.getValue(DoorBlock.POWERED))
            .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)

        level.setBlockAndUpdate(lowerPos, newLower)
        level.setBlockAndUpdate(upperPos, newUpper)
    }
}
