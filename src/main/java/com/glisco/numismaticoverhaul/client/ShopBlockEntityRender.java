package com.glisco.numismaticoverhaul.client;

import com.glisco.numismaticoverhaul.block.ShopBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ShopBlockEntityRender implements BlockEntityRenderer<ShopBlockEntity, ShopBlockEntityRender.ShopRenderState> {

    private final ItemModelResolver itemModelResolver;

    public ShopBlockEntityRender(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public ShopRenderState createRenderState() {
        return new ShopRenderState();
    }

    @Override
    public void extractRenderState(ShopBlockEntity blockEntity, ShopRenderState state, float tickDelta, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        state.gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        ItemStack item = blockEntity.getItemToRender();
        if (!item.isEmpty()) {
            this.itemModelResolver.updateForNonLiving(state.itemRenderState, item, ItemDisplayContext.FIXED, null);
        }
        // No need to clear; ItemStackRenderState defaults to empty
    }

    @Override
    public void submit(ShopRenderState state, PoseStack matrices, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
        if (state.itemRenderState.isEmpty()) return;
        matrices.pushPose();
        matrices.translate(0.5, 0.75, 0.5);
        long gameTime = state.gameTime;
        matrices.mulPose(Axis.YP.rotationDegrees(gameTime % 360));
        float bob = (float) Math.sin((gameTime % 60) / 60.0 * Math.PI * 2) * 0.05f;
        matrices.translate(0.0, bob, 0.0);
        matrices.scale(0.5f, 0.5f, 0.5f);
        state.itemRenderState.submit(matrices, nodeCollector, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        matrices.popPose();
    }

    public static class ShopRenderState extends BlockEntityRenderState {
        public final ItemStackRenderState itemRenderState = new ItemStackRenderState();
        public long gameTime;
    }
}
