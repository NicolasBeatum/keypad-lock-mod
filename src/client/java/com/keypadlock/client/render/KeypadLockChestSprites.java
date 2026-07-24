package com.keypadlock.client.render;

import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Sprites del cofre reforzado (una sola mitad reusada para left/right por
 * ahora). IMPORTANTE: no arma el SpriteId a mano -- el sprite real que
 * registra el atlas de cofres (fuente "minecraft:directory" en
 * atlases/chests.json, prefix "entity/chest/") queda con el path
 * "entity/chest/<nombre>", NO "<nombre>" pelado. Sheets.CHEST_MAPPER es
 * exactamente el SpriteMapper que vanilla usa para aplicar ese prefijo
 * (CHEST_REGULAR = ChestSpecialRenderer.REGULAR.map(CHEST_MAPPER::apply)) --
 * reusarlo aca evita tener que hardcodear "entity/chest/" nosotros.
 */
public final class KeypadLockChestSprites {

    private static final MultiblockChestResources<Identifier> NAMES = new MultiblockChestResources<>(
        Identifier.fromNamespaceAndPath("keypadlock", "reinforced_chest"),
        Identifier.fromNamespaceAndPath("keypadlock", "reinforced_chest_left"),
        Identifier.fromNamespaceAndPath("keypadlock", "reinforced_chest_right")
    );

    private static final MultiblockChestResources<SpriteId> SPRITES = NAMES.map(Sheets.CHEST_MAPPER::apply);

    private KeypadLockChestSprites() {
    }

    public static SpriteId choose(ChestType type) {
        return SPRITES.select(type);
    }
}
