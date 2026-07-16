package Infinitygroup.microtech.mixin.client;

import Infinitygroup.microtech.item.TechArmorFlightVisualHelper;
import Infinitygroup.microtech.item.TechArmorFlightVisualHelper.FlightVisualState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(PlayerRenderer.class)
public abstract class TechArmorFlightRendererMixin {
    private static final double MIN_HORIZONTAL_SPEED_SQR = 0.03D * 0.03D;
    private static final float MAX_PITCH_DEGREES = 35.0F;
    private static final float MAX_BANK_DEGREES = 18.0F;
    private static final float GLIDE_BLEND_SPEED = 0.18F;
    private static final Map<UUID, Float> GLIDE_BLEND_BY_PLAYER = new HashMap<>();

    @Inject(
            method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("TAIL")
    )
    private void microtech$applyTechArmorGlideRotation(AbstractClientPlayer player, PoseStack poseStack, float ageInTicks, float bodyYaw, float partialTick, float headYaw, CallbackInfo ci) {
        FlightVisualState flightVisualState = TechArmorFlightVisualHelper.getFlightVisualState(player);
        if (flightVisualState == FlightVisualState.FLIGHT_NONE) {
            GLIDE_BLEND_BY_PLAYER.remove(player.getUUID());
            return;
        }

        applyGlideRotation(player, poseStack, flightVisualState == FlightVisualState.FLIGHT_GLIDE, player.getUUID());
    }

    private static void applyGlideRotation(AbstractClientPlayer player, PoseStack poseStack, boolean gliding, UUID playerId) {
        float targetBlend = gliding ? 1.0F : 0.0F;
        float currentBlend = GLIDE_BLEND_BY_PLAYER.getOrDefault(playerId, targetBlend);
        float glideBlend = Mth.lerp(GLIDE_BLEND_SPEED, currentBlend, targetBlend);
        GLIDE_BLEND_BY_PLAYER.put(playerId, glideBlend);

        Vec3 movement = player.getDeltaMovement();
        Vec3 horizontalMovement = new Vec3(movement.x, 0.0D, movement.z);
        double horizontalSpeedSqr = horizontalMovement.lengthSqr();
        if (horizontalSpeedSqr < MIN_HORIZONTAL_SPEED_SQR) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 moveDir = horizontalMovement.normalize();
        Vec3 lookDir = horizontalLook.normalize();
        float alignment = (float) Mth.clamp(moveDir.dot(lookDir), -1.0D, 1.0D);
        float turnSign = (float) Math.signum(moveDir.x * lookDir.z - moveDir.z * lookDir.x);
        float turnAngle = (float) Math.toDegrees(Math.acos(alignment));

        float speedFactor = Mth.clamp((float) Math.sqrt(horizontalSpeedSqr) / 0.20F, 0.0F, 1.0F);
        float pitchDegrees = Mth.clamp(-18.0F - (float) (movement.y * 18.0D), -MAX_PITCH_DEGREES, 18.0F) * glideBlend;
        float bankDegrees = Mth.clamp(turnSign * turnAngle * 0.28F, -MAX_BANK_DEGREES, MAX_BANK_DEGREES) * glideBlend;

        float smoothedPitch = pitchDegrees * speedFactor;
        float smoothedBank = bankDegrees * speedFactor;

        poseStack.mulPose(Axis.XP.rotationDegrees(smoothedPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(smoothedBank));
    }
}
