package Infinitygroup.microtech.entity.control;

import Infinitygroup.microtech.Config;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ControlledMobCommandTargetService {
    public static final int PRIORITY_CONTROLLER_ATTACKED = 100;
    public static final int PRIORITY_CONTROLLER_ATTACKED_TARGET = 80;
    public static final int PRIORITY_CONTROLLED_ALLY_ATTACKED = 60;

    private static final Map<UUID, CommandedTarget> TARGETS_BY_CONTROLLER = new HashMap<>();

    private ControlledMobCommandTargetService() {
    }

    public static void rememberControllerDamage(ServerPlayer controller, LivingEntity target, String origin) {
        remember(controller, target, PRIORITY_CONTROLLER_ATTACKED_TARGET, origin);
    }

    public static void rememberControllerAttacker(ServerPlayer controller, LivingEntity attacker, String origin) {
        remember(controller, attacker, PRIORITY_CONTROLLER_ATTACKED, origin);
    }

    public static void rememberControlledAllyAttacker(UUID controllerId, ServerLevel level, LivingEntity attacker, String origin) {
        ServerPlayer controller = level.getServer().getPlayerList().getPlayer(controllerId);
        if (controller != null) {
            remember(controller, attacker, PRIORITY_CONTROLLED_ALLY_ATTACKED, origin);
        }
    }

    public static void remember(ServerPlayer controller, LivingEntity target, int priority, String origin) {
        if (!isValidCommandTarget(controller, target)) {
            return;
        }

        long now = controller.level().getGameTime();
        CommandedTarget existing = TARGETS_BY_CONTROLLER.get(controller.getUUID());
        if (existing != null && existing.expiresAt() > now && existing.priority() > priority) {
            return;
        }

        TARGETS_BY_CONTROLLER.put(controller.getUUID(), new CommandedTarget(
                controller.getUUID(),
                target.getUUID(),
                target.level().dimension(),
                now,
                now + Config.controlledTargetCommandDuration,
                priority,
                origin
        ));
    }

    public static Optional<LivingEntity> getCommandedTarget(Mob controlledMob, ServerPlayer controller) {
        CommandedTarget command = TARGETS_BY_CONTROLLER.get(controller.getUUID());
        if (command == null) {
            return Optional.empty();
        }

        long now = controlledMob.level().getGameTime();
        if (command.expiresAt() <= now || command.dimension() != controlledMob.level().dimension()) {
            TARGETS_BY_CONTROLLER.remove(controller.getUUID());
            return Optional.empty();
        }

        Entity entity = ((ServerLevel) controlledMob.level()).getEntity(command.target());
        if (!(entity instanceof LivingEntity target) || !isValidForControlledMob(controlledMob, controller, target)) {
            TARGETS_BY_CONTROLLER.remove(controller.getUUID());
            return Optional.empty();
        }

        return Optional.of(target);
    }

    public static Optional<CommandedTarget> getCommand(UUID controller) {
        return Optional.ofNullable(TARGETS_BY_CONTROLLER.get(controller));
    }

    public static void clearIfTarget(UUID controller, LivingEntity target) {
        CommandedTarget command = TARGETS_BY_CONTROLLER.get(controller);
        if (command != null && command.target().equals(target.getUUID())) {
            TARGETS_BY_CONTROLLER.remove(controller);
        }
    }

    public static void cleanup(Level level) {
        if (level.isClientSide()) {
            return;
        }

        long now = level.getGameTime();
        TARGETS_BY_CONTROLLER.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    private static boolean isValidCommandTarget(ServerPlayer controller, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved() || target == controller) {
            return false;
        }

        if (controller.level().dimension() != target.level().dimension()) {
            return false;
        }

        if (controller.distanceToSqr(target) > Config.controlledTargetMaximumDistance * Config.controlledTargetMaximumDistance) {
            return false;
        }

        if (target instanceof Player player && (player.getAbilities().instabuild || player.isSpectator())) {
            return false;
        }

        return target.attackable() && !ControlledMobAllianceService.hasSameController(controller, target);
    }

    private static boolean isValidForControlledMob(Mob controlledMob, ServerPlayer controller, LivingEntity target) {
        if (controlledMob.distanceToSqr(target) > Config.controlledTargetMaximumDistance * Config.controlledTargetMaximumDistance) {
            return false;
        }

        return ControlledMobAllianceService.canControlledMobTarget(controlledMob, target)
                && !ControlledMobAllianceService.isProtectedAlly(controlledMob, target)
                && !ControlledMobAllianceService.hasSameController(controller, target);
    }

    public record CommandedTarget(
            UUID controller,
            UUID target,
            ResourceKey<Level> dimension,
            long issuedAt,
            long expiresAt,
            int priority,
            String origin
    ) {
    }
}
