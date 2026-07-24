package com.keypadlock.block

import com.keypadlock.KeypadLockMod
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour

object ModBlocks {
    lateinit var REINFORCED_CHEST: ReinforcedChestBlock
        private set
    lateinit var REINFORCED_IRON_DOOR: ReinforcedIronDoorBlock
        private set

    fun register() {
        val chestKey: ResourceKey<net.minecraft.world.level.block.Block> =
            ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "reinforced_chest"))
        // strength(-1f, ...) = indestructible a mano/pico (como bedrock), en
        // cualquier modo de juego. explosionResistance ~40 sobrevive TNT (power 4),
        // creeper cargado (power 6), etc, pero una explosion de radio/poder ~10+
        // ya alcanza a destruirlo (formula de decaimiento de Explosion: cada bloque
        // atravesado resta (resistencia+0.3)*0.3 al poder restante).
        val chestProps = BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).setId(chestKey).strength(-1.0f, 40.0f)
        REINFORCED_CHEST = ReinforcedChestBlock(
            { ModBlockEntities.REINFORCED_CHEST_TYPE },
            SoundEvents.CHEST_OPEN,
            SoundEvents.CHEST_CLOSE,
            chestProps
        )
        Registry.register(BuiltInRegistries.BLOCK, chestKey, REINFORCED_CHEST)

        val doorKey: ResourceKey<net.minecraft.world.level.block.Block> =
            ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "reinforced_iron_door"))
        val doorProps = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).setId(doorKey).strength(-1.0f, 40.0f)
        REINFORCED_IRON_DOOR = ReinforcedIronDoorBlock(doorProps)
        Registry.register(BuiltInRegistries.BLOCK, doorKey, REINFORCED_IRON_DOOR)
    }
}
