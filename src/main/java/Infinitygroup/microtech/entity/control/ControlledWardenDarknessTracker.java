package Infinitygroup.microtech.entity.control;

import Infinitygroup.microtech.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ControlledWardenDarknessTracker {
    private static final List<DarknessPulse> PULSES = new ArrayList<>();

    private ControlledWardenDarknessTracker() {
    }

    public static void recordPulse(Warden warden, Entity controller, int radius) {
        if (warden.level().isClientSide()) {
            return;
        }

        long now = warden.level().getGameTime();
        PULSES.add(new DarknessPulse(
                warden.getUUID(),
                controller.getUUID(),
                warden.position(),
                radius,
                now,
                now + 8L
        ));
    }

    public static boolean wasRecentlyEmittedByControlledWardenAtProtectedTarget(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        long now = serverLevel.getGameTime();
        cleanup(serverLevel);
        for (DarknessPulse pulse : PULSES) {
            if (pulse.expiresAt() < now) {
                continue;
            }
            if (pulse.position().distanceToSqr(target.position()) <= pulse.radius() * pulse.radius()
                    && isProtectedByPulseController(serverLevel, pulse, target)) {
                return true;
            }
        }
        return false;
    }

    public static Optional<DarknessPulse> getLastPulse(UUID warden) {
        for (int i = PULSES.size() - 1; i >= 0; i--) {
            DarknessPulse pulse = PULSES.get(i);
            if (pulse.warden().equals(warden)) {
                return Optional.of(pulse);
            }
        }
        return Optional.empty();
    }

    public static void cleanup(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<DarknessPulse> iterator = PULSES.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAt() <= now) {
                iterator.remove();
            }
        }
    }

    private static boolean isProtectedByPulseController(ServerLevel level, DarknessPulse pulse, LivingEntity target) {
        Optional<UUID> targetOwner = ControlledMobAllianceService.ownerOrController(target);
        if (targetOwner.isPresent() && targetOwner.get().equals(pulse.controller())) {
            return true;
        }

        if (Config.controlledBossProtectTeamMembers) {
            ServerPlayer controller = level.getServer().getPlayerList().getPlayer(pulse.controller());
            return controller != null && controller.isAlliedTo(target);
        }

        return false;
    }

    public record DarknessPulse(UUID warden, UUID controller, Vec3 position, int radius, long gameTime, long expiresAt) {
    }
}
