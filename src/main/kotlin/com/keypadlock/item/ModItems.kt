package com.keypadlock.item

import com.keypadlock.KeypadLockMod
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item

object ModItems {
    lateinit var LOCK: LockItem
        private set
    lateinit var KEY: KeyItem
        private set

    fun register() {
        val lockKey: ResourceKey<Item> = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "lock"))
        LOCK = LockItem(Item.Properties().setId(lockKey).stacksTo(64))
        Registry.register(BuiltInRegistries.ITEM, lockKey, LOCK)

        val keyKey: ResourceKey<Item> = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(KeypadLockMod.MOD_ID, "key"))
        KEY = KeyItem(Item.Properties().setId(keyKey).stacksTo(1))
        Registry.register(BuiltInRegistries.ITEM, keyKey, KEY)
    }
}
