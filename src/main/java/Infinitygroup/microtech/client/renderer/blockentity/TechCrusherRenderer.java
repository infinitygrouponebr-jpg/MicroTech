package Infinitygroup.microtech.client.renderer.blockentity;

import Infinitygroup.microtech.block.entity.TechCrusherBlockEntity;
import Infinitygroup.microtech.client.model.block.TechCrusherModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TechCrusherRenderer extends GeoBlockRenderer<TechCrusherBlockEntity> {
    public TechCrusherRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
        super(new TechCrusherModel());
    }

    @Override
    public void render(TechCrusherBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        if (blockEntity.getLevel() == null || !blockEntity.isProcessing() || blockEntity.getProgress() <= 0) {
            return;
        }

        ItemStack visualStack = blockEntity.getInputStack().copy();
        if (visualStack.isEmpty()) {
            return;
        }
        visualStack.setCount(1);

        float progress = (blockEntity.getProgress() + partialTick) / (float) TechCrusherBlockEntity.PROCESS_TICKS;
        float orbitAngle = progress * 360.0F * 2.5F;
        double orbitRadians = Math.toRadians(orbitAngle);
        double orbitRadius = 0.23D;
        double orbitX = Math.cos(orbitRadians) * orbitRadius;
        double orbitZ = Math.sin(orbitRadians) * orbitRadius;
        double bob = Math.sin(orbitRadians * 2.0D) * 0.05D;

        poseStack.pushPose();
        poseStack.translate(0.5D + orbitX, 1.08D + bob, 0.5D + orbitZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(orbitAngle + 90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(72.0F));
        poseStack.scale(0.45F, 0.45F, 0.45F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                visualStack,
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
