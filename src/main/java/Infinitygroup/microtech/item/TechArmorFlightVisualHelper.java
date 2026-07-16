package Infinitygroup.microtech.item;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;

public final class TechArmorFlightVisualHelper {
    private static final int FLIGHT_COST_PER_TICK = 20;
    private static final double FORWARD_DOT_THRESHOLD = 0.5D;
    private static final double MIN_HORIZONTAL_SPEED_SQR = 0.03D * 0.03D;

    private TechArmorFlightVisualHelper() {
    }

    public static boolean hasFlightChipInChestplate(Player player) {
        return player != null && TechArmorUpgradeHelper.hasFlightChip(TechArmorEnergyHelper.getChestplate(player));
    }

    public static boolean hasChestplateEnergyForFlight(Player player) {
        return player != null && TechArmorEnergyHelper.getChestplateEnergy(player) > 0;
    }

    public static boolean hasChestplateEnergyForFlightTick(Player player) {
        return player != null && TechArmorEnergyHelper.getChestplateEnergy(player) >= FLIGHT_COST_PER_TICK;
    }

    public static boolean isTechFlightActive(Player player) {
        return player != null
                && !player.isSpectator()
                && TechArmorEnergyHelper.isFullTechArmorSet(player)
                && hasFlightChipInChestplate(player)
                && hasChestplateEnergyForFlightTick(player)
                && player.getAbilities().flying;
    }

    public static boolean shouldRenderElytraPose(Player player) {
        return shouldUseFlightPose(player);
    }

    public static boolean shouldUseFlightPose(Player player) {
        return getFlightVisualState(player) == FlightVisualState.FLIGHT_GLIDE;
    }

    public static FlightVisualState getFlightVisualState(Player player) {
        if (!isTechFlightActive(player)) {
            return FlightVisualState.FLIGHT_NONE;
        }

        return isFlyingForward(player) ? FlightVisualState.FLIGHT_GLIDE : FlightVisualState.FLIGHT_HOVER;
    }

    public static boolean isFlyingForward(Player player) {
        if (!isTechFlightActive(player)) {
            return false;
        }

        Vec3 movement = player.getDeltaMovement();
        Vec3 horizontalMovement = new Vec3(movement.x, 0.0D, movement.z);
        if (horizontalMovement.lengthSqr() < MIN_HORIZONTAL_SPEED_SQR) {
            return false;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4D) {
            return false;
        }

        Vec3 movementDir = horizontalMovement.normalize();
        Vec3 lookDir = horizontalLook.normalize();
        double dot = Mth.clamp(movementDir.dot(lookDir), -1.0D, 1.0D);
        return dot > FORWARD_DOT_THRESHOLD;
    }

    public enum FlightVisualState {
        FLIGHT_NONE,
        FLIGHT_HOVER,
        FLIGHT_GLIDE
    }
}
