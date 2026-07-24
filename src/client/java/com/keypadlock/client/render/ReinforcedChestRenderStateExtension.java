package com.keypadlock.client.render;

/**
 * Interfaz "duck" agregada via Mixin a ChestRenderState -- marca si el
 * render state en curso corresponde a un cofre reforzado, para que
 * ChestRendererMixin sepa cuando reemplazar el sprite vanilla por el nuestro.
 */
public interface ReinforcedChestRenderStateExtension {
    boolean keypadlock$isReinforced();

    void keypadlock$setReinforced(boolean value);
}
