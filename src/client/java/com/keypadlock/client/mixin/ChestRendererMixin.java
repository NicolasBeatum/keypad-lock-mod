package com.keypadlock.client.mixin;

import com.keypadlock.block.ReinforcedChestBlockEntity;
import com.keypadlock.client.render.KeypadLockChestSprites;
import com.keypadlock.client.render.ReinforcedChestRenderStateExtension;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ChestRenderer.getChestMaterial() decide la textura via instanceof checks
 * hardcodeados (TrappedChestBlockEntity, EnderChestBlockEntity,
 * CopperChestBlock...) sobre un enum ChestMaterialType cerrado -- no hay
 * forma de agregarle un caso "reinforced" sin tocar bytecode. En vez de eso:
 * marcamos el ChestRenderState (via ChestRenderStateMixin) cuando el block
 * entity es un ReinforcedChestBlockEntity, y en submit() reemplazamos el
 * SpriteId elegido por Sheets.chooseSprite() por el nuestro. `require = 0`
 * en ambos injectors: si el descriptor deja de matchear en una futura build
 * de MC, esto se desactiva en silencio (vuelve a verse como cofre vanilla)
 * en vez de tumbar el juego entero.
 */
@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("TAIL"),
        require = 0
    )
    private void keypadlock$markReinforced(
        BlockEntity blockEntity,
        ChestRenderState renderState,
        float partialTick,
        Vec3 cameraPos,
        ModelFeatureRenderer.CrumblingOverlay crumbling,
        CallbackInfo ci
    ) {
        boolean reinforced = blockEntity instanceof ReinforcedChestBlockEntity;
        ((ReinforcedChestRenderStateExtension) (Object) renderState).keypadlock$setReinforced(reinforced);
    }

    @Redirect(
        method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Sheets;chooseSprite(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState$ChestMaterialType;Lnet/minecraft/world/level/block/state/properties/ChestType;)Lnet/minecraft/client/resources/model/sprite/SpriteId;"
        ),
        require = 0
    )
    private SpriteId keypadlock$chooseSprite(
        ChestRenderState.ChestMaterialType material,
        ChestType type,
        ChestRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        if (((ReinforcedChestRenderStateExtension) (Object) state).keypadlock$isReinforced()) {
            return KeypadLockChestSprites.choose(type);
        }
        return Sheets.chooseSprite(material, type);
    }
}
