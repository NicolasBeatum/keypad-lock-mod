package com.keypadlock.block

import com.keypadlock.KeypadLockMod
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BlockEntityType

object ModBlockEntities {
    lateinit var REINFORCED_CHEST_TYPE: BlockEntityType<ReinforcedChestBlockEntity>
        private set
    lateinit var REINFORCED_DOOR_TYPE: BlockEntityType<ReinforcedDoorBlockEntity>
        private set

    fun register() {
        REINFORCED_CHEST_TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "reinforced_chest"),
            BlockEntityType(
                { pos, state -> ReinforcedChestBlockEntity(pos, state) },
                setOf(ModBlocks.REINFORCED_CHEST)
            )
        )

        REINFORCED_DOOR_TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "reinforced_iron_door"),
            BlockEntityType(
                { pos, state -> ReinforcedDoorBlockEntity(pos, state) },
                setOf(ModBlocks.REINFORCED_IRON_DOOR)
            )
        )
    }
}
