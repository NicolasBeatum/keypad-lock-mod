package com.keypadlock.client

import com.keypadlock.block.ModBlockEntities
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.minecraft.client.renderer.blockentity.ChestRenderer

/**
 * Registra el renderer del cofre reforzado. NOTA (recorte de alcance
 * deliberado): reutiliza el ChestRenderer vanilla (mismo modelo/atlas que un
 * cofre normal) en lugar de tejer `reinforced_chest.png` dentro del sprite
 * atlas de cofres -- eso requeriria extender `Sheets.CHEST_MAPPER` /
 * `MultiblockChestResources`, que son practicamente internos y no tienen un
 * punto de extension publico simple. La textura reforzada ya esta generada
 * en `assets/keypadlock/textures/entity/chest/reinforced_chest.png` para
 * cablearla mas adelante (resource pack o mixin) si se necesita.
 */
object ClientBlockEntityRenderers {
    fun register() {
        BlockEntityRendererRegistry.register(ModBlockEntities.REINFORCED_CHEST_TYPE) { ctx -> ChestRenderer(ctx) }
    }
}
