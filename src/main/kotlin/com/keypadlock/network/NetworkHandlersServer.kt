package com.keypadlock.network

import com.keypadlock.block.ReinforcedChestBlock
import com.keypadlock.block.ReinforcedChestBlockEntity
import com.keypadlock.block.ReinforcedDoorBlockEntity
import com.keypadlock.block.ReinforcedIronDoorBlock
import com.keypadlock.block.ReinforcementOps
import com.keypadlock.util.PasswordHasher
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.phys.Vec3

object NetworkHandlersServer {
    private const val MAX_REACH_SQ = 8.0 * 8.0

    fun register() {
        ServerPlayNetworking.registerGlobalReceiver(SubmitPasswordPayload.TYPE) { payload, context ->
            val player = context.player()
            context.server().execute { handleSubmitPassword(player, payload) }
        }
        ServerPlayNetworking.registerGlobalReceiver(CancelKeypadPayload.TYPE) { payload, context ->
            val player = context.player()
            context.server().execute { handleCancel(player, payload) }
        }
    }

    /**
     * Si el jugador cancela ANTES de terminar de crear la contraseña (passwordHash
     * todavia null), revertir el refuerzo en vez de dejar el bloque bloqueado para
     * siempre sin forma de abrirlo. El candado ya se gasto -- eso no se devuelve --
     * pero el cofre/puerta vuelve a ser utilizable normalmente.
     */
    private fun handleCancel(player: ServerPlayer, payload: CancelKeypadPayload) {
        val level = player.level()
        val pos = payload.blockPos
        val state = level.getBlockState(pos)

        when (state.block) {
            is ReinforcedChestBlock -> {
                val be = level.getBlockEntity(pos) as? ReinforcedChestBlockEntity ?: return
                if (be.owner == player.uuid && be.passwordHash == null) {
                    ReinforcementOps.revertChest(level, pos)
                    player.sendSystemMessage(Component.literal("Cancelado -- el cofre vuelve a ser normal (el candado se perdió)."))
                }
            }
            is ReinforcedIronDoorBlock -> {
                val be = level.getBlockEntity(pos) as? ReinforcedDoorBlockEntity ?: return
                if (be.owner == player.uuid && be.passwordHash == null) {
                    ReinforcementOps.revertDoor(level, pos)
                    player.sendSystemMessage(Component.literal("Cancelado -- la puerta vuelve a ser normal (el candado se perdió)."))
                }
            }
            else -> {}
        }
    }

    fun sendOpenKeypad(player: ServerPlayer, pos: BlockPos, mode: KeypadMode) {
        ServerPlayNetworking.send(player, OpenKeypadPayload(pos, mode))
    }

    /** Nunca confiamos ciegamente en el cliente: re-validamos todo server-side. */
    private fun handleSubmitPassword(player: ServerPlayer, payload: SubmitPasswordPayload) {
        val level = player.level()
        val pos = payload.blockPos

        if (player.position().distanceToSqr(Vec3.atCenterOf(pos)) > MAX_REACH_SQ) return
        if (!PasswordHasher.isValidLength(payload.digits)) {
            player.sendSystemMessage(Component.literal("Contraseña invalida (4 a 8 digitos, del 0 al 9)."))
            return
        }

        val state = level.getBlockState(pos)
        when (state.block) {
            is ReinforcedChestBlock -> handleChest(player, pos, payload)
            is ReinforcedIronDoorBlock -> handleDoor(player, pos, payload)
            else -> {
                // El bloque ya no es un bloque reforzado valido (removido, etc.) -- ignorar.
            }
        }
    }

    private fun handleChest(player: ServerPlayer, pos: BlockPos, payload: SubmitPasswordPayload) {
        val level = player.level()
        val be = level.getBlockEntity(pos) as? ReinforcedChestBlockEntity ?: return

        if (payload.forCreate) {
            if (be.owner != player.uuid || be.passwordHash != null) return
            val hash = PasswordHasher.hash(payload.digits)
            be.passwordHash = hash
            be.setChanged()

            // Cofre doble: replicar la misma contraseña/dueño en la otra mitad
            // para que ambas se abran/cierren como una sola unidad.
            val state = level.getBlockState(pos)
            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                val otherPos = ChestBlock.getConnectedBlockPos(pos, state)
                val otherBe = level.getBlockEntity(otherPos) as? ReinforcedChestBlockEntity
                if (otherBe != null && otherBe.owner == be.owner) {
                    otherBe.passwordHash = hash
                    otherBe.setChanged()
                }
            }

            player.sendSystemMessage(Component.literal("Contraseña creada para el cofre."))
            return
        }

        if (PasswordHasher.matches(payload.digits, be.passwordHash)) {
            // Usar el bloque (no el block entity) para abrir el menu: asi un
            // cofre DOBLE reforzado combina ambas mitades en un contenedor de
            // 54 slots igual que un cofre doble vanilla, en vez de abrir solo
            // los 27 slots de la mitad clickeada. Abrir el menu ya reemplaza el
            // screen del cliente solo -- no hace falta mandar CloseKeypadPayload.
            val block = level.getBlockState(pos).block as? ReinforcedChestBlock ?: return
            block.openMenuFor(player, level, pos)
        } else if (payload.explicit) {
            // Intento automatico (sin Enter) fallido: no decimos nada, el jugador
            // puede seguir tipeando. Solo avisamos en un intento explicito.
            player.sendSystemMessage(Component.literal("Contraseña incorrecta."))
        }
    }

    private fun handleDoor(player: ServerPlayer, pos: BlockPos, payload: SubmitPasswordPayload) {
        val level = player.level()
        val be = level.getBlockEntity(pos) as? ReinforcedDoorBlockEntity ?: return

        if (payload.forCreate) {
            if (be.owner != player.uuid || be.passwordHash != null) return
            be.passwordHash = PasswordHasher.hash(payload.digits)
            be.setChanged()
            player.sendSystemMessage(Component.literal("Contraseña creada para la puerta."))
            return
        }

        if (!PasswordHasher.matches(payload.digits, be.passwordHash)) {
            if (payload.explicit) player.sendSystemMessage(Component.literal("Contraseña incorrecta."))
            return
        }

        val lowerState = level.getBlockState(pos)
        if (lowerState.block !is ReinforcedIronDoorBlock) return
        val door = lowerState.block as ReinforcedIronDoorBlock
        door.setOpen(player, level, lowerState, pos, true)
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.1f + 0.9f)
        // A diferencia del cofre, aca no hay un "menu" que reemplace el screen
        // del cliente solo -- hay que avisarle explicitamente que cierre el keypad.
        ServerPlayNetworking.send(player, CloseKeypadPayload(pos))
    }
}
