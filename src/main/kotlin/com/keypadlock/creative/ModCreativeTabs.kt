package com.keypadlock.creative

import com.keypadlock.item.ModItems
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab

/** Registra el candado y la llave en la pestana vanilla "Ingredients" para que sean obtenibles en-game. */
object ModCreativeTabs {
    fun register() {
        val ingredients: ResourceKey<CreativeModeTab> =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("ingredients"))
        CreativeModeTabEvents.modifyOutputEvent(ingredients).register { output ->
            output.accept(ModItems.LOCK.defaultInstance, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
            output.accept(ModItems.KEY.defaultInstance, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
        }
    }
}
