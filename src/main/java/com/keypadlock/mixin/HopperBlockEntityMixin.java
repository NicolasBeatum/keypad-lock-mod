package com.keypadlock.mixin;

import com.keypadlock.block.ReinforcedChestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Niega la extraccion automatica desde un cofre reforzado. La inyeccion se
 * hace antes de que vanilla combine un cofre doble en CompoundContainer, por
 * lo que cubre ambas mitades y no afecta menus de jugadores ni comandos.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(
        method = "getSourceContainer",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void keypadlock$blockExtraction(
        Level level,
        Hopper hopper,
        BlockPos sourcePos,
        BlockState sourceState,
        CallbackInfoReturnable<net.minecraft.world.Container> cir
    ) {
        if (sourceState.getBlock() instanceof ReinforcedChestBlock) {
            cir.setReturnValue(null);
        }
    }
}
