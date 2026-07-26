package Infinitygroup.microtech.entity.control;

import Infinitygroup.microtech.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.WitherSkull;

import java.util.Optional;
import java.util.UUID;

public final class ControlledMobAllianceService {
    private ControlledMobAllianceService() {
    }

    public static boolean isController(Entity entity, Player player) {
        return resolveController(entity)
                .map(controller -> controller.equals(player.getUUID()))
                .orElse(false);
    }

    public static boolean hasSameController(Entity first, Entity second) {
        Optional<UUID> firstController = ownerOrController(first);
        Optional<UUID> secondController = ownerOrController(second);
        return firstController.isPresent()
                && secondController.isPresent()
                && firstController.get().equals(secondController.get());
    }

    public static boolean isProtectedAlly(Entity controlledEntity, Entity target) {
        if (controlledEntity == null || target == null || controlledEntity == target) {
            return true;
        }

        Optional<UUID> controller = resolveController(controlledEntity);
        if (controller.isEmpty()) {
            return false;
        }

        UUID controllerId = controller.get();
        Optional<UUID> targetOwner = ownerOrController(target);
        if (targetOwner.isPresent() && targetOwner.get().equals(controllerId)) {
            return true;
        }

        if (Config.controlledBossProtectTeamMembers && target instanceof LivingEntity livingTarget) {
            ServerPlayer controllerPlayer = controlledEntity.level().getServer() == null
                    ? null
                    : controlledEntity.level().getServer().getPlayerList().getPlayer(controllerId);
            return controllerPlayer != null && controllerPlayer.isAlliedTo(livingTarget);
        }

        return false;
    }

    public static boolean canControlledMobTarget(Mob controlledMob, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved() || controlledMob == target) {
            return false;
        }

        if (controlledMob.level().dimension() != target.level().dimension()) {
            return false;
        }

        if (target instanceof Player player && (player.getAbilities().instabuild || player.isSpectator())) {
            return false;
        }

        return target.attackable() && !isProtectedAlly(controlledMob, target);
    }

    public static boolean canControlledMobDamage(Entity controlledSource, LivingEntity target) {
        if (controlledSource == null || target == null) {
            return true;
        }

        if (isFriendlyFireAllowed(controlledSource)) {
            return true;
        }

        Entity source = ControlledTemporaryEntityTracker.resolveCreator(controlledSource).orElse(controlledSource);
        return !isProtectedAlly(source, target);
    }

    public static Optional<UUID> resolveController(Entity controlledEntity) {
        if (controlledEntity == null) {
            return Optional.empty();
        }

        Optional<UUID> tracked = ControlledTemporaryEntityTracker.getController(controlledEntity);
        if (tracked.isPresent()) {
            return tracked;
        }

        return ControlledMobData.getController(controlledEntity);
    }

    public static Optional<UUID> ownerOrController(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        Optional<UUID> controller = resolveController(entity);
        if (controller.isPresent()) {
            return controller;
        }

        if (entity instanceof Player player) {
            return Optional.of(player.getUUID());
        }

        Optional<UUID> temporaryController = ControlledTemporaryEntityTracker.getController(entity);
        if (temporaryController.isPresent()) {
            return temporaryController;
        }

        if (entity instanceof OwnableEntity ownable) {
            UUID owner = ownable.getOwnerUUID();
            if (owner != null) {
                return Optional.of(owner);
            }
        }

        return Optional.empty();
    }

    public static boolean isFriendlyFireAllowed(Entity controlledSource) {
        if (ControlledTemporaryEntityTracker.isCreatedByControlledWither(controlledSource)
                || controlledSource instanceof WitherBoss
                || controlledSource instanceof WitherSkull
                || controlledSource instanceof Projectile projectile && projectile.getOwner() instanceof WitherBoss) {
            return Config.controlledWitherFriendlyFire || Config.controlledWitherDamagesAllies || Config.controlledBossFriendlyFire;
        }

        if (controlledSource instanceof Mob mob && ControlledMobManager.isAdvancedControlledBoss(mob)) {
            return Config.controlledBossFriendlyFire;
        }

        return Config.friendlyFire;
    }
}
