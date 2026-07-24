package com.keypadlock.block

import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.UUID

/**
 * Puerta reforzada: solo la mitad inferior (`DoubleBlockHalf.LOWER`) carga
 * este block entity -- la mitad superior nunca deberia tener uno propio,
 * siempre se resuelve hacia abajo (igual que vanilla trata OPEN/POWERED
 * como sincronizados entre las dos mitades).
 */
class ReinforcedDoorBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.REINFORCED_DOOR_TYPE, pos, state) {

    var owner: UUID? = null
    var passwordHash: String? = null

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null)
        passwordHash = input.getString("PasswordHash").orElse(null)
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        owner?.let { output.store("Owner", UUIDUtil.CODEC, it) }
        passwordHash?.let { output.putString("PasswordHash", it) }
    }
}
