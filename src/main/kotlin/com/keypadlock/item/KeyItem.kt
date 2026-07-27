package com.keypadlock.item

import com.keypadlock.block.ReinforcedChestBlock
import com.keypadlock.block.ReinforcedChestBlockEntity
import com.keypadlock.block.ReinforcedDoorBlockEntity
import com.keypadlock.block.ReinforcedIronDoorBlock
import com.keypadlock.block.ReinforcementOps
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import java.util.function.Consumer

/**
 * Llave: revierte un cofre o puerta reforzada a la version vanilla, SOLO si
 * el jugador es el `owner` guardado en el block entity. No se consume (es
 * reutilizable). No requiere password -- la llave prueba propiedad, no
 * conocimiento de la contraseña. El swap de bloque real vive en
 * [ReinforcementOps] (compartido con la cancelacion de password sin confirmar).
 */
class KeyItem(properties: Properties) : Item(properties) {

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipDisplay: TooltipDisplay,
        add: Consumer<Component>,
        flag: TooltipFlag
    ) {
        for (i in 1..4) {
            add.accept(Component.translatable("item.keypadlock.key.lore$i").withStyle(ChatFormatting.GRAY))
        }
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        if (level.isClientSide) return InteractionResult.SUCCESS
        val player = context.player as? ServerPlayer ?: return InteractionResult.PASS
        val pos = context.clickedPos
        val state = level.getBlockState(pos)

        val handled = when (state.block) {
            is ReinforcedChestBlock -> unreinforceChest(player, pos)
            is ReinforcedIronDoorBlock -> unreinforceDoor(player, pos)
            else -> false
        }
        return if (handled) InteractionResult.CONSUME else InteractionResult.PASS
    }

    private fun unreinforceChest(player: ServerPlayer, pos: BlockPos): Boolean {
        val level = player.level()
        val be = level.getBlockEntity(pos) as? ReinforcedChestBlockEntity ?: return false
        if (be.owner != player.uuid) {
            player.sendSystemMessage(Component.literal("No eres el dueño de este candado."))
            return true
        }

        ReinforcementOps.revertChest(level, pos)

        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0f, 1.2f)
        player.sendSystemMessage(Component.literal("Candado retirado del cofre."))
        return true
    }

    private fun unreinforceDoor(player: ServerPlayer, clickedPos: BlockPos): Boolean {
        val level = player.level()
        val clickedState = level.getBlockState(clickedPos)
        val lowerPos = if (clickedState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) clickedPos else clickedPos.below()
        val be = level.getBlockEntity(lowerPos) as? ReinforcedDoorBlockEntity ?: return false
        if (be.owner != player.uuid) {
            player.sendSystemMessage(Component.literal("No eres el dueño de este candado."))
            return true
        }

        ReinforcementOps.revertDoor(level, lowerPos)

        level.playSound(null, lowerPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0f, 1.2f)
        player.sendSystemMessage(Component.literal("Candado retirado de la puerta."))
        return true
    }
}
