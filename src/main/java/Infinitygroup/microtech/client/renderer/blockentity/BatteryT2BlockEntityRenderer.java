package Infinitygroup.microtech.client.renderer.blockentity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.BatteryT2BlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BatteryT2BlockEntityRenderer implements BlockEntityRenderer<BatteryT2BlockEntity> {
    public BatteryT2BlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BatteryT2BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = blockEntity.getChargingStack();
        if (stack.isEmpty() || blockEntity.getLevel() == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.28D, 0.5D);

        float scale = stack.is(Microtech.TECH_SWORD.get()) ? 0.26F : 0.45F;
        poseStack.scale(scale, scale, scale);

        float spinSpeed = switch (blockEntity.getChargingStatus()) {
            case BatteryT2BlockEntity.STATUS_CHARGING -> 18.0F;
            case BatteryT2BlockEntity.STATUS_FULL -> 6.0F;
            default -> 12.0F;
        };
        float rotation = (float)((blockEntity.getLevel().getGameTime() + partialTick) * spinSpeed) % 360.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
        );
        poseStack.popPose();
    }
}
