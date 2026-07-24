package com.keypadlock.block

import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.UUID

/**
 * Cofre reforzado: extiende ChestBlockEntity (mismo inventario/animacion de
 * tapa/render vanilla) y le agrega `owner` + `passwordHash`. El hash es lo
 * UNICO relacionado a la password que se persiste -- ver [com.keypadlock.util.PasswordHasher].
 */
class ReinforcedChestBlockEntity(pos: BlockPos, state: BlockState) :
    ChestBlockEntity(ModBlockEntities.REINFORCED_CHEST_TYPE, pos, state) {

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
