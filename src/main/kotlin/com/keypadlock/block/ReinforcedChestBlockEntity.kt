package com.keypadlock.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.UUIDUtil
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.UUID

/**
 * Cofre reforzado: extiende ChestBlockEntity (mismo inventario/animacion de
 * tapa/render vanilla) y le agrega `owner` + `passwordHash`. El hash es lo
 * UNICO relacionado a la password que se persiste -- ver [com.keypadlock.util.PasswordHasher].
 *
 * Implementa WorldlyContainer para que una tolva NUNCA pueda extraer items
 * (canTakeItemThroughFace siempre false), sin importar la contraseña -- las
 * tolvas no "entran" por la puerta del keypad, asi que no deberian poder
 * vaciar el cofre igual. Insertar SI se permite (una tolva puede seguir
 * llenandolo desde afuera si el jugador quiere automatizar eso).
 */
class ReinforcedChestBlockEntity(pos: BlockPos, state: BlockState) :
    ChestBlockEntity(ModBlockEntities.REINFORCED_CHEST_TYPE, pos, state), WorldlyContainer {

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

    override fun getSlotsForFace(side: Direction): IntArray = IntArray(containerSize) { it }

    override fun canPlaceItemThroughFace(index: Int, stack: ItemStack, direction: Direction?): Boolean =
        canPlaceItem(index, stack)

    override fun canTakeItemThroughFace(index: Int, stack: ItemStack, direction: Direction): Boolean = false
}
