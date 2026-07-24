package com.keypadlock.item

import com.keypadlock.block.ModBlocks
import com.keypadlock.block.ReinforcedChestBlockEntity
import com.keypadlock.block.ReinforcedDoorBlockEntity
import com.keypadlock.network.KeypadMode
import com.keypadlock.network.NetworkHandlersServer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

/**
 * Candado: refuerza un cofre o puerta de hierro VANILLA (sin reforzar
 * todavia). Soporta cofres simples y dobles -- si el cofre ya forma parte de
 * un cofre doble vanilla, se refuerzan AMBAS mitades a la vez con el mismo
 * dueño (la contraseña, una vez creada, tambien se replica a la otra mitad).
 * Todo el trabajo real ocurre server-side.
 */
class LockItem(properties: Properties) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        if (level.isClientSide) return InteractionResult.SUCCESS
        val player = context.player as? ServerPlayer ?: return InteractionResult.PASS
        val pos = context.clickedPos
        val state = level.getBlockState(pos)

        val handled = when {
            state.block === Blocks.CHEST -> reinforceChest(context, player, pos)
            state.block === Blocks.IRON_DOOR -> reinforceDoor(context, player, pos)
            else -> false
        }
        return if (handled) InteractionResult.CONSUME else InteractionResult.PASS
    }

    private fun reinforceChest(context: UseOnContext, player: ServerPlayer, pos: BlockPos): Boolean {
        val level = context.level
        val state = level.getBlockState(pos)
        val isDouble = state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
        val otherPos = if (isDouble) ChestBlock.getConnectedBlockPos(pos, state) else null
        val otherState = otherPos?.let { level.getBlockState(it) }
        if (isDouble && (otherState == null || otherState.block !== Blocks.CHEST)) {
            // Estado vecino inconsistente (cofre bloqueado, gato encima, etc.) -- no arriesgar.
            player.sendSystemMessage(Component.literal("No se puede reforzar este cofre doble ahora mismo."))
            return true
        }

        convertChestHalf(level, pos, state, player)
        if (otherPos != null && otherState != null) {
            convertChestHalf(level, otherPos, otherState, player)
        }

        consumeIfNotCreative(context)
        NetworkHandlersServer.sendOpenKeypad(player, pos, KeypadMode.CREATE)
        val msg = if (isDouble) "Cofre doble reforzado. Crea una contraseña en el teclado."
                  else "Cofre reforzado. Crea una contraseña en el teclado."
        player.sendSystemMessage(Component.literal(msg))
        return true
    }

    /** Convierte una sola mitad de cofre (simple o parte de uno doble) a reforzada, preservando su inventario propio. */
    private fun convertChestHalf(level: net.minecraft.world.level.Level, pos: BlockPos, state: net.minecraft.world.level.block.state.BlockState, player: ServerPlayer) {
        val oldBe = level.getBlockEntity(pos) as? ChestBlockEntity ?: return
        val items = (0 until oldBe.containerSize).map { oldBe.getItem(it).copy() }
        // BlockEntity.preRemoveSideEffects() dropea automaticamente el contenido de
        // cualquier BlockEntity que sea Container cuando el bloque cambia (lo llama
        // LevelChunk.setBlockState antes de reemplazar el block entity). Si no
        // vaciamos el viejo ANTES de setBlockAndUpdate, vanilla tira los items al
        // piso Y nosotros los volvemos a poner en el nuevo cofre -> duplicados.
        oldBe.clearContent()

        val newState = ModBlocks.REINFORCED_CHEST.defaultBlockState()
            .setValue(ChestBlock.FACING, state.getValue(ChestBlock.FACING))
            .setValue(ChestBlock.TYPE, state.getValue(ChestBlock.TYPE))
            .setValue(ChestBlock.WATERLOGGED, state.getValue(ChestBlock.WATERLOGGED))
        level.setBlockAndUpdate(pos, newState)

        val newBe = level.getBlockEntity(pos) as? ReinforcedChestBlockEntity ?: return
        items.forEachIndexed { i, stack -> newBe.setItem(i, stack) }
        newBe.owner = player.uuid
        newBe.passwordHash = null
        newBe.setChanged()
    }

    private fun reinforceDoor(context: UseOnContext, player: ServerPlayer, clickedPos: BlockPos): Boolean {
        val level = context.level
        val clickedState = level.getBlockState(clickedPos)
        val lowerPos = if (clickedState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) clickedPos else clickedPos.below()
        val lowerState = level.getBlockState(lowerPos)
        val upperPos = lowerPos.above()
        val upperState = level.getBlockState(upperPos)
        if (lowerState.block !== Blocks.IRON_DOOR) return false

        val newLower = ModBlocks.REINFORCED_IRON_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, lowerState.getValue(DoorBlock.FACING))
            .setValue(DoorBlock.HINGE, lowerState.getValue(DoorBlock.HINGE))
            .setValue(DoorBlock.OPEN, lowerState.getValue(DoorBlock.OPEN))
            .setValue(DoorBlock.POWERED, lowerState.getValue(DoorBlock.POWERED))
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
        val newUpper = newLower
            .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
            .setValue(DoorBlock.HINGE, upperState.getValue(DoorBlock.HINGE))

        level.setBlockAndUpdate(lowerPos, newLower)
        level.setBlockAndUpdate(upperPos, newUpper)

        val newBe = level.getBlockEntity(lowerPos) as? ReinforcedDoorBlockEntity ?: return true
        newBe.owner = player.uuid
        newBe.passwordHash = null
        newBe.setChanged()

        consumeIfNotCreative(context)
        NetworkHandlersServer.sendOpenKeypad(player, lowerPos, KeypadMode.CREATE)
        player.sendSystemMessage(Component.literal("Puerta reforzada. Crea una contraseña en el teclado."))
        return true
    }

    private fun consumeIfNotCreative(context: UseOnContext) {
        val player = context.player ?: return
        if (!player.isCreative) {
            context.itemInHand.shrink(1)
        }
    }
}
