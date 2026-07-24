package com.keypadlock.client.mixin;

import com.keypadlock.client.render.ReinforcedChestRenderStateExtension;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChestRenderState.class)
public class ChestRenderStateMixin implements ReinforcedChestRenderStateExtension {

    @Unique
    private boolean keypadlock$reinforced = false;

    @Override
    public boolean keypadlock$isReinforced() {
        return this.keypadlock$reinforced;
    }

    @Override
    public void keypadlock$setReinforced(boolean value) {
        this.keypadlock$reinforced = value;
    }
}
