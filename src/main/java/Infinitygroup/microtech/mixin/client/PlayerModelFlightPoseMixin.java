package Infinitygroup.microtech.mixin.client;

import Infinitygroup.microtech.item.TechArmorFlightVisualHelper;
import Infinitygroup.microtech.item.TechArmorFlightVisualHelper.FlightVisualState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class PlayerModelFlightPoseMixin<T extends LivingEntity> {
    private static final float FLIGHT_ARM_XROT = 0.42F;
    private static final float FLIGHT_ARM_ZROT = 0.08F;
    private static final float FLIGHT_LEG_XROT = 0.08F;
    private static final float FLIGHT_LEG_ZROT = 0.02F;

    @Shadow @Final public ModelPart hat;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftLeg;
    @Shadow @Final public ModelPart rightLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void microtech$applyFlightPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }

        if (TechArmorFlightVisualHelper.getFlightVisualState(player) != FlightVisualState.FLIGHT_GLIDE) {
            return;
        }

        applyGlidePose();
    }

    private void applyGlidePose() {
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;

        this.rightArm.xRot = FLIGHT_ARM_XROT;
        this.leftArm.xRot = FLIGHT_ARM_XROT;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.rightArm.zRot = -FLIGHT_ARM_ZROT;
        this.leftArm.zRot = FLIGHT_ARM_ZROT;

        this.rightLeg.xRot = 0.0F;
        this.leftLeg.xRot = FLIGHT_LEG_XROT;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.rightLeg.zRot = -FLIGHT_LEG_ZROT;
        this.leftLeg.zRot = FLIGHT_LEG_ZROT;
        this.hat.copyFrom(this.head);

        if ((Object) this instanceof PlayerModel<?> playerModel) {
            playerModel.jacket.copyFrom(this.body);
            playerModel.leftSleeve.copyFrom(this.leftArm);
            playerModel.rightSleeve.copyFrom(this.rightArm);
            playerModel.leftPants.copyFrom(this.leftLeg);
            playerModel.rightPants.copyFrom(this.rightLeg);
        }
    }
}
