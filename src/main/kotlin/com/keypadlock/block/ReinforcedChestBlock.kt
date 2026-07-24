package com.keypadlock.block

import com.keypadlock.network.KeypadMode
import com.keypadlock.network.NetworkHandlersServer
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.server.level.ServerPlayer
import java.util.function.Supplier

/**
 * Cofre reforzado. Soporta cofres simples y dobles -- dos ReinforcedChestBlock
 * vecinos se combinan igual que dos cofres vanilla (chestCanConnectTo por
 * defecto en ChestBlock ya solo permite conectar con `state.is(this)`, o sea
 * SOLO con otro cofre reforzado, nunca con un cofre vanilla suelto). Toda
 * apertura requiere password via keypad, sin excepcion (no hay "sesion" que
 * quede desbloqueada).
 */
class ReinforcedChestBlock(
    entityTypeSupplier: Supplier<BlockEntityType<out ChestBlockEntity>>,
    openSound: SoundEvent,
    closeSound: SoundEvent,
    properties: BlockBehaviour.Properties
) : ChestBlock(entityTypeSupplier, openSound, closeSound, properties) {

    // IMPORTANTE: en MC 26.2, ChestBlock.newBlockEntity() esta hardcodeado a
    // `new ChestBlockEntity(pos, state)` (BlockEntityType.CHEST vanilla) y
    // NO usa el entityTypeSupplier del constructor pese a que ese supplier
    // si es usado correctamente por blockEntityType()/getTicker()/combine().
    // Sin este override, el bloque termina con un ChestBlockEntity vanilla
    // dentro de un bloque keypadlock:reinforced_chest -> crash de
    // validateBlockState al cargar el chunk. Hay que crear el block entity
    // reforzado nosotros mismos.
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ReinforcedChestBlockEntity(pos, state)

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        val serverPlayer = player as? ServerPlayer ?: return InteractionResult.FAIL
        NetworkHandlersServer.sendOpenKeypad(serverPlayer, pos, KeypadMode.OPEN_CHEST)
        return InteractionResult.CONSUME
    }

    /**
     * Abre el menu correcto tras validar la contraseña -- reusa
     * ChestBlock.getMenuProvider()/combine() (protected) para que un cofre
     * DOBLE reforzado abra el contenedor de 54 slots fusionado, igual que
     * un cofre doble vanilla, en vez del contenedor de 27 slots de una sola
     * mitad.
     */
    fun openMenuFor(player: ServerPlayer, level: Level, pos: BlockPos) {
        val state = level.getBlockState(pos)
        val provider = getMenuProvider(state, level, pos) ?: return
        player.openMenu(provider)
        player.awardStat(getOpenChestStat())
    }
}
