package Infinitygroup.microtech.client.renderer.blockentity;

import Infinitygroup.microtech.block.entity.TechTableBlockEntity;
import Infinitygroup.microtech.client.model.block.TechTableModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TechTableRenderer extends GeoBlockRenderer<TechTableBlockEntity> {
    private static final double DISPLAY_BASE_Y = 2.16D;

    public TechTableRenderer(BlockEntityRendererProvider.Context context) {
        super(new TechTableModel());
    }

    @Override
    public void render(TechTableBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        ItemStack displayStack = blockEntity.getDisplayStack();
        if (displayStack.isEmpty() || blockEntity.getLevel() == null) {
            return;
        }

        float bob = (float) Math.sin((blockEntity.getLevel().getGameTime() + partialTick) * 0.12F) * 0.025F;
        float rotation = (blockEntity.getLevel().getGameTime() + partialTick) * 1.8F;
        double swayX = 0.0D;
        double swayZ = 0.0D;
        if (blockEntity.getDisplayShakeTicks() > 0) {
            float intensity = blockEntity.getDisplayShakeStrength();
            float shakeTime = blockEntity.getDisplayShakeTicks() + partialTick;
            bob += (float) Math.sin(shakeTime * 1.8F) * 0.05F * intensity;
            rotation += (float) Math.sin(shakeTime * 2.2F) * 5.0F * intensity;
            int hitQuality = blockEntity.getLastHitQualityId();
            if (hitQuality == 0) {
                bob += (float) Math.abs(Math.sin(shakeTime * 3.2F)) * 0.05F * intensity;
                rotation += (float) Math.sin(shakeTime * 4.0F) * 6.0F * intensity;
            } else if (hitQuality == 1) {
                bob += (float) Math.abs(Math.sin(shakeTime * 2.8F)) * 0.03F * intensity;
                rotation += (float) Math.sin(shakeTime * 3.2F) * 3.0F * intensity;
            } else {
                swayX += Math.sin(shakeTime * 2.6F) * 0.04D * intensity;
                swayZ += Math.cos(shakeTime * 2.0F) * 0.025D * intensity;
                rotation += (float) Math.sin(shakeTime * 2.0F) * 7.0F * intensity;
                bob -= 0.01F * intensity;
            }
        }

        if (blockEntity.getSessionInstability() > 0 && blockEntity.isWorking()) {
            double wobble = blockEntity.getSessionInstability() * 0.0015D;
            swayX += Math.sin((blockEntity.getSessionTicks() + partialTick) * 0.35F) * wobble;
            swayZ += Math.cos((blockEntity.getSessionTicks() + partialTick) * 0.31F) * wobble;
            rotation += (float) Math.sin((blockEntity.getSessionTicks() + partialTick) * 0.24F) * blockEntity.getSessionInstability() * 0.4F;
        }

        ItemStack renderedStack = displayStack.copy();
        renderedStack.setCount(1);

        poseStack.pushPose();
        poseStack.translate(0.5D + swayX, DISPLAY_BASE_Y + bob, 0.5D + swayZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(0.40F, 0.40F, 0.40F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                renderedStack,
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
