package Infinitygroup.microtech.client.renderer.blockentity;

import Infinitygroup.microtech.block.entity.ElectricFurnaceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class ElectricFurnaceRenderer implements BlockEntityRenderer<ElectricFurnaceBlockEntity> {
    public ElectricFurnaceRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ElectricFurnaceBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null || !blockEntity.isProcessing()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.01D, 0.5D);
        poseStack.scale(0.52F, 0.52F, 0.52F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.FIRE.defaultBlockState(),
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();

        ItemStack input = blockEntity.getInputStack();
        if (input.isEmpty()) {
            return;
        }

        ItemStack visualStack = input.copy();
        visualStack.setCount(1);

        float progress = (blockEntity.getProgress() + partialTick) / (float) ElectricFurnaceBlockEntity.MAX_PROGRESS;
        float spin = progress * 360.0F * 1.75F;
        double bob = Math.sin(progress * Math.PI * 2.0D) * 0.04D;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.45D + bob, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(22.0F));
        poseStack.scale(0.52F, 0.52F, 0.52F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                visualStack,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
        );
        poseStack.popPose();
    }
}
