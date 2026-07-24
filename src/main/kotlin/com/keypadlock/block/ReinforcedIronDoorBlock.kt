package com.keypadlock.block

import com.keypadlock.network.KeypadMode
import com.keypadlock.network.NetworkHandlersServer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.phys.BlockHitResult

/**
 * Puerta de hierro reforzada, 2 bloques de alto igual que vanilla. El block
 * entity (owner + passwordHash) vive UNICAMENTE en la mitad inferior --
 * cualquier interaccion en la mitad superior se resuelve hacia abajo.
 */
class ReinforcedIronDoorBlock(properties: BlockBehaviour.Properties) :
    DoorBlock(BlockSetType.IRON, properties), EntityBlock {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER) return null
        return ReinforcedDoorBlockEntity(pos, state)
    }

    /** Resuelve cualquier posicion de la puerta (mitad superior o inferior) hacia la mitad inferior. */
    fun lowerHalfPos(state: BlockState, pos: BlockPos): BlockPos =
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) pos else pos.below()

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        val serverPlayer = player as? ServerPlayer ?: return InteractionResult.FAIL
        val lowerPos = lowerHalfPos(state, pos)

        if (state.getValue(OPEN)) {
            // Ya esta abierta: cerrarla directamente, sin password (comportamiento vanilla).
            val lowerState = level.getBlockState(lowerPos)
            setOpen(player, level, lowerState, lowerPos, false)
            level.playSound(null, lowerPos, SoundEvents.IRON_DOOR_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.1f + 0.9f)
            return InteractionResult.CONSUME
        }

        // Cerrada: siempre pide password via keypad (la logica de la llave del
        // dueno se resuelve antes, en KeyItem.useOn, que intercepta la
        // interaccion antes de que este metodo se llame).
        NetworkHandlersServer.sendOpenKeypad(serverPlayer, lowerPos, KeypadMode.OPEN_DOOR)
        return InteractionResult.CONSUME
    }
}
