package Infinitygroup.microtech.entity.control;

import Infinitygroup.microtech.Config;
import Infinitygroup.microtech.Microtech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ControlledMobManager {
    private static final Map<Mob, InstalledGoals> INSTALLED_GOALS = new java.util.WeakHashMap<>();
    private static final Map<UUID, Set<UUID>> CONTROLLED_BY_OWNER = new HashMap<>();
    private static final Map<UUID, RecentThreat> RECENT_ATTACKERS = new HashMap<>();
    private static final Map<UUID, RecentThreat> RECENT_OWNER_TARGETS = new HashMap<>();
    private static final int CONTROL_TICK_INTERVAL = 20;
    private static final double APPLY_DISTANCE_SQR = 64.0D;

    private ControlledMobManager() {
    }

    public static ApplyResult tryInstall(Mob mob, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        ApplyResult result = canInstall(mob, player);
        if (!result.success()) {
            return result;
        }

        ControlledMobData.install(mob, player.getUUID());
        addControlledIndex(player.getUUID(), mob.getUUID());
        installGoals(mob);
        debug("Controlled mob installed: mob={} role={} controller={}", mob.getType().toShortString(), ControlledMobCombatManager.getRole(mob), player.getGameProfile().getName());
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        if (Config.consumeChipOnUse && !player.getAbilities().instabuild) {
            stack.shrink(1);
            player.setItemInHand(hand, stack);
        }
        playInstallFeedback(mob);
        player.displayClientMessage(ComponentHelper.installed(mob), true);
        return ApplyResult.SUCCESS;
    }

    public static boolean removeChip(Mob mob, ServerPlayer player) {
        if (!ControlledMobData.isControlled(mob)) {
            return false;
        }
        if (!ControlledMobData.isControlledBy(mob, player.getUUID()) && !player.hasPermissions(2)) {
            player.displayClientMessage(ComponentHelper.notController(), true);
            return false;
        }

        ControlledMobData.getController(mob).ifPresent(owner -> removeControlledIndex(owner, mob.getUUID()));
        ControlledMobData.remove(mob);
        mob.setTarget(null);
        mob.getNavigation().stop();
        giveOrDropChip(player);
        playRemoveFeedback(mob);
        player.displayClientMessage(ComponentHelper.removed(mob), true);
        return true;
    }

    public static void installGoals(Mob mob) {
        if (!ControlledMobData.isControlled(mob) || INSTALLED_GOALS.containsKey(mob)) {
            return;
        }
        ControlledFollowGoal followGoal = new ControlledFollowGoal(mob);
        ControlledTargetGoal targetGoal = new ControlledTargetGoal(mob);
        mob.goalSelector.addGoal(3, followGoal);
        mob.targetSelector.addGoal(2, targetGoal);
        INSTALLED_GOALS.put(mob, new InstalledGoals(followGoal, targetGoal));
        ControlledMobData.getController(mob).ifPresent(owner -> addControlledIndex(owner, mob.getUUID()));
    }

    public static void onMobUnloaded(Mob mob) {
        INSTALLED_GOALS.remove(mob);
    }

    public static void onControlledMobRemoved(Mob mob) {
        ControlledMobData.getController(mob).ifPresent(owner -> removeControlledIndex(owner, mob.getUUID()));
        removeGoals(mob);
    }

    public static void rememberDamage(LivingEntity victim, Entity directAttacker, long gameTime) {
        if (!(directAttacker instanceof LivingEntity attacker) || attacker == victim) {
            return;
        }
        if (victim instanceof ServerPlayer player) {
            RECENT_ATTACKERS.put(player.getUUID(), new RecentThreat(attacker.getUUID(), gameTime + Config.controllerChipThreatMemoryTicks));
        }
        if (attacker instanceof ServerPlayer player) {
            RECENT_OWNER_TARGETS.put(player.getUUID(), new RecentThreat(victim.getUUID(), gameTime + Config.controllerChipThreatMemoryTicks));
        }
        ControlledMobData.getController(victim).ifPresent(owner -> {
            RECENT_ATTACKERS.put(owner, new RecentThreat(attacker.getUUID(), gameTime + Config.controllerChipThreatMemoryTicks));
            debug("Threat registered for {} from {}", owner, attacker.getType().toShortString());
        });
    }

    public static boolean areAllies(LivingEntity first, LivingEntity second) {
        if (first == second) {
            return true;
        }
        Optional<UUID> firstOwner = ownerOrController(first);
        Optional<UUID> secondOwner = ownerOrController(second);
        if (first.isAlliedTo(second) || second.isAlliedTo(first)) {
            return true;
        }
        if (firstOwner.isEmpty() || secondOwner.isEmpty()) {
            return false;
        }
        return firstOwner.get().equals(secondOwner.get());
    }

    public static boolean isValidTarget(Mob mob, LivingEntity target, ServerPlayer controller, double radius) {
        if (target == null || !target.isAlive() || target == controller || target.isSpectator()) {
            return false;
        }
        if (target instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        if (mob.distanceToSqr(target) > radius * radius) {
            return false;
        }
        if (areAllies(controller, target) || areAllies(mob, target)) {
            return false;
        }
        return ControlledMobBehaviorRegistry.get(mob).canKeepTarget(mob, target, controller);
    }

    public static boolean isConfiguredAllowed(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (Config.controllerChipEntityDenylist.contains(id)) {
            return false;
        }
        if (!Config.controllerChipAllowVillagers && mob instanceof Villager) {
            return false;
        }
        if (!Config.controllerChipEntityAllowlist.isEmpty() && !Config.controllerChipEntityAllowlist.contains(id)) {
            return false;
        }
        if (!Config.allowBosses && isBossDenied(mob)) {
            return false;
        }
        if (!Config.allowModdedMobs && !Microtech.MODID.equals(id.getNamespace()) && !"minecraft".equals(id.getNamespace())) {
            return false;
        }
        if (mob instanceof Enemy) {
            return Config.allowHostileMobs;
        }
        if (mob instanceof Animal || mob.getType().getCategory() == MobCategory.CREATURE || mob.getType().getCategory() == MobCategory.WATER_CREATURE) {
            return Config.allowPassiveMobs;
        }
        return Config.allowNeutralMobs;
    }

    public static boolean hasExternalOwner(Mob mob) {
        if (mob instanceof TamableAnimal tamable && tamable.isTame()) {
            return true;
        }
        if (mob instanceof AbstractHorse horse && horse.isTamed()) {
            return true;
        }
        if (mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return true;
        }
        return ReflectiveOwnerLookup.hasOwner(mob);
    }

    public static void tickControlledVisuals(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel) || mob.tickCount % 80 != 0) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + mob.getBbHeight() * 0.75D, mob.getZ(), 2, 0.25D, 0.15D, 0.25D, 0.01D);
    }

    private static ApplyResult canInstall(Mob mob, ServerPlayer player) {
        if (!Config.enableControllerChip) {
            return ApplyResult.disabled();
        }
        if (!mob.isAlive() || mob.distanceToSqr(player) > APPLY_DISTANCE_SQR) {
            return ApplyResult.invalid();
        }
        if (ControlledMobData.isControlled(mob) || hasExternalOwner(mob)) {
            return ApplyResult.hasOwner();
        }
        if (!isConfiguredAllowed(mob)) {
            return ApplyResult.incompatible();
        }
        if (countControlled(player.getUUID()) >= Config.maxControlledMobsPerPlayer) {
            return ApplyResult.limit();
        }
        return ApplyResult.SUCCESS;
    }

    private static Optional<UUID> ownerOrController(LivingEntity entity) {
        Optional<UUID> controlled = ControlledMobData.getController(entity);
        if (controlled.isPresent()) {
            return controlled;
        }
        if (entity instanceof Player player) {
            return Optional.of(player.getUUID());
        }
        if (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return Optional.of(ownable.getOwnerUUID());
        }
        return Optional.empty();
    }

    private static ServerPlayer getController(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return ControlledMobData.getController(mob)
                .map(id -> serverLevel.getServer().getPlayerList().getPlayer(id))
                .orElse(null);
    }

    private static LivingEntity findTarget(Mob mob, ServerPlayer controller) {
        double radius = Config.defendRadius;
        ControlledMobCombatRole role = ControlledMobCombatManager.getRole(mob);
        if (!ControlledMobCombatManager.canAttack(role)) {
            return null;
        }

        LivingEntity remembered = getRememberedTarget(mob, controller, radius);
        if (remembered != null) {
            return remembered;
        }

        ControlledMobOrder order = ControlledMobData.getOrder(mob);
        Vec3 center = order == ControlledMobOrder.GUARD
                ? ControlledMobData.getGuardPos(mob).map(Vec3::atCenterOf).orElse(mob.position())
                : mob.position();
        AABB area = new AABB(center, center).inflate(radius);
        LivingEntity current = mob.getTarget();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : mob.level().getEntitiesOfClass(LivingEntity.class, area, entity -> isValidTarget(mob, entity, controller, radius))) {
            if (candidate instanceof Mob candidateMob) {
                LivingEntity candidateTarget = candidateMob.getTarget();
                boolean attackingController = candidateTarget == controller;
                boolean attackingAlly = Config.controlledMobsHelpEachOther
                        && candidateTarget instanceof Mob targetMob
                        && ControlledMobData.isControlledBy(targetMob, controller.getUUID());
                if (attackingController || attackingAlly) {
                    return candidate;
                }
            }
            if (candidate == current) {
                best = candidate;
                bestDistance = -1.0D;
                continue;
            }
            if (order == ControlledMobOrder.GUARD && candidate instanceof Enemy) {
                double distance = mob.distanceToSqr(candidate);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static LivingEntity getRememberedTarget(Mob mob, ServerPlayer controller, double radius) {
        long gameTime = mob.level().getGameTime();
        for (Map<UUID, RecentThreat> map : java.util.List.of(RECENT_ATTACKERS, RECENT_OWNER_TARGETS)) {
            RecentThreat threat = map.get(controller.getUUID());
            if (threat == null || threat.expiresAt() < gameTime) {
                map.remove(controller.getUUID());
                continue;
            }
            Entity entity = ((ServerLevel) mob.level()).getEntity(threat.entityId());
            if (entity instanceof LivingEntity living && isValidTarget(mob, living, controller, radius)) {
                return living;
            }
        }
        return null;
    }

    private static void tickMovement(Mob mob, ServerPlayer controller) {
        ControlledMobOrder order = ControlledMobData.getOrder(mob);
        if (order == ControlledMobOrder.FOLLOW) {
            moveNear(mob, controller.position(), Config.startFollowingDistance, Config.followDistance, controller);
            return;
        }
        if (order == ControlledMobOrder.STAY) {
            Optional<BlockPos> stayPos = ControlledMobData.getStayPos(mob);
            Optional<ResourceKey<Level>> stayDimension = ControlledMobData.getStayDimension(mob);
            if (stayPos.isPresent() && stayDimension.map(mob.level().dimension()::equals).orElse(false) && mob.getTarget() == null) {
                moveNear(mob, Vec3.atCenterOf(stayPos.get()), 4.0D, 1.5D, controller);
            }
            return;
        }
        if (order == ControlledMobOrder.GUARD) {
            Optional<BlockPos> guardPos = ControlledMobData.getGuardPos(mob);
            Optional<ResourceKey<Level>> guardDimension = ControlledMobData.getGuardDimension(mob);
            if (guardPos.isPresent() && guardDimension.map(mob.level().dimension()::equals).orElse(false) && mob.getTarget() == null) {
                Vec3 guardCenter = Vec3.atCenterOf(guardPos.get());
                moveNear(mob, guardCenter, 5.0D, 2.0D, controller);
            }
        }
    }

    private static void moveNear(Mob mob, Vec3 target, double startDistance, double stopDistance, ServerPlayer controller) {
        double distanceSqr = mob.distanceToSqr(target);
        if (distanceSqr > Config.teleportDistance * Config.teleportDistance) {
            tryRecoveryTeleport(mob, controller);
            return;
        }
        if (distanceSqr > startDistance * startDistance) {
            mob.getNavigation().moveTo(target.x(), target.y(), target.z(), 1.1D);
        } else if (distanceSqr < stopDistance * stopDistance) {
            mob.getNavigation().stop();
        }
    }

    private static void tryRecoveryTeleport(Mob mob, ServerPlayer controller) {
        if (ControlledMobData.getTeleportCooldown(mob) > 0) {
            return;
        }
        if (mob.level().dimension() != controller.level().dimension()) {
            if (!Config.allowCrossDimensionTeleport) {
                return;
            }
            return;
        }
        if (mob.isPassenger() || mob.isLeashed() || !(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int attempt = 0; attempt < 12; attempt++) {
            int dx = mob.getRandom().nextInt(9) - 4;
            int dz = mob.getRandom().nextInt(9) - 4;
            BlockPos base = controller.blockPosition().offset(dx, 0, dz);
            BlockPos pos = findSafeTeleportPos(mob, serverLevel, base);
            if (pos != null) {
                mob.getNavigation().stop();
                mob.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                ControlledMobData.setTeleportCooldown(mob, 100);
                return;
            }
        }
    }

    private static BlockPos findSafeTeleportPos(Mob mob, ServerLevel level, BlockPos base) {
        if (!level.hasChunkAt(base)) {
            return null;
        }
        for (int yOffset = 2; yOffset >= -4; yOffset--) {
            BlockPos pos = base.offset(0, yOffset, 0);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            boolean wantsWater = mob.isInWaterOrBubble() || mob.getType().getCategory() == MobCategory.WATER_CREATURE || mob.getType().getCategory() == MobCategory.WATER_AMBIENT;
            if (wantsWater) {
                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    return pos;
                }
                continue;
            }
            if (level.getBlockState(pos.below()).isAir() || level.getBlockState(pos.below()).is(Blocks.WATER)) {
                continue;
            }
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private static boolean isBossDenied(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return Config.controllerChipBossDenylist.contains(id) || mob.getType() == EntityType.WITHER || mob.getType() == EntityType.ENDER_DRAGON;
    }

    private static void playInstallFeedback(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, mob.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(), 12, 0.35D, 0.35D, 0.35D, 0.03D);
    }

    private static void playRemoveFeedback(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, mob.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.2F);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(), 8, 0.25D, 0.25D, 0.25D, 0.02D);
    }

    private static void giveOrDropChip(ServerPlayer player) {
        ItemStack chip = new ItemStack(Microtech.CONTROLLER_CHIP.get());
        if (!player.getInventory().add(chip)) {
            player.drop(chip, false);
        }
    }

    private static void addControlledIndex(UUID owner, UUID mob) {
        CONTROLLED_BY_OWNER.computeIfAbsent(owner, ignored -> new HashSet<>()).add(mob);
    }

    private static void removeControlledIndex(UUID owner, UUID mob) {
        Set<UUID> mobs = CONTROLLED_BY_OWNER.get(owner);
        if (mobs == null) {
            return;
        }
        mobs.remove(mob);
        if (mobs.isEmpty()) {
            CONTROLLED_BY_OWNER.remove(owner);
        }
    }

    private static int countControlled(UUID owner) {
        Set<UUID> mobs = CONTROLLED_BY_OWNER.get(owner);
        return mobs == null ? 0 : mobs.size();
    }

    private static void removeGoals(Mob mob) {
        InstalledGoals goals = INSTALLED_GOALS.remove(mob);
        if (goals == null) {
            return;
        }
        mob.goalSelector.removeGoal(goals.followGoal());
        mob.targetSelector.removeGoal(goals.targetGoal());
    }

    private record RecentThreat(UUID entityId, long expiresAt) {
    }

    private record InstalledGoals(Goal followGoal, Goal targetGoal) {
    }

    public record ApplyResult(boolean success, String messageKey) {
        public static final ApplyResult SUCCESS = new ApplyResult(true, "");

        private static ApplyResult disabled() {
            return new ApplyResult(false, "message.microtech.controller_chip.disabled");
        }

        private static ApplyResult invalid() {
            return new ApplyResult(false, "message.microtech.controller_chip.invalid");
        }

        private static ApplyResult hasOwner() {
            return new ApplyResult(false, "message.microtech.controller_chip.has_owner");
        }

        private static ApplyResult incompatible() {
            return new ApplyResult(false, "message.microtech.controller_chip.incompatible");
        }

        private static ApplyResult limit() {
            return new ApplyResult(false, "message.microtech.controller_chip.limit");
        }
    }

    private static final class ControlledFollowGoal extends Goal {
        private final Mob mob;

        private ControlledFollowGoal(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return ControlledMobData.isControlled(this.mob) && shouldFollowGoalRun(this.mob);
        }

        @Override
        public boolean canContinueToUse() {
            return ControlledMobData.isControlled(this.mob) && shouldFollowGoalRun(this.mob);
        }

        @Override
        public void tick() {
            ServerPlayer controller = getController(this.mob);
            ControlledMobData.tickTeleportCooldown(this.mob);
            if (controller == null) {
                this.mob.getNavigation().stop();
                return;
            }
            ControlledMobSupport.tick(this.mob, controller);
            this.mob.getLookControl().setLookAt(controller, 10.0F, this.mob.getMaxHeadXRot());
            tickMovement(this.mob, controller);
            tickControlledVisuals(this.mob);
        }
    }

    private static boolean shouldFollowGoalRun(Mob mob) {
        ControlledMobCombatRole role = ControlledMobCombatManager.getRole(mob);
        if (role == ControlledMobCombatRole.SUPPORT || role == ControlledMobCombatRole.NONE) {
            return true;
        }
        LivingEntity target = mob.getTarget();
        return target == null || !target.isAlive();
    }

    private static final class ControlledTargetGoal extends Goal {
        private final Mob mob;
        private int cooldown;

        private ControlledTargetGoal(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return ControlledMobData.isControlled(this.mob);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            ServerPlayer controller = getController(this.mob);
            if (controller == null) {
                this.mob.setTarget(null);
                return;
            }
            ControlledMobBehaviorRegistry.get(this.mob).tick(this.mob, controller);
            LivingEntity current = this.mob.getTarget();
            if (current != null && !isValidTarget(this.mob, current, controller, Config.defendRadius)) {
                this.mob.setTarget(null);
            }
            if (this.cooldown-- > 0) {
                return;
            }
            this.cooldown = CONTROL_TICK_INTERVAL;
            LivingEntity target = findTarget(this.mob, controller);
            if (target != null && target != this.mob.getTarget()) {
                this.mob.setTarget(target);
                ControlledMobBehaviorRegistry.get(this.mob).onTargetSelected(this.mob, target, controller);
                debug("Target selected: mob={} role={} target={}", this.mob.getType().toShortString(), ControlledMobCombatManager.getRole(this.mob), target.getType().toShortString());
            }
        }
    }

    private static void debug(String message, Object... args) {
        if (Config.controllerChipDebug) {
            org.slf4j.LoggerFactory.getLogger(ControlledMobManager.class).info("[ControllerChip] " + message, args);
        }
    }

    private static final class ReflectiveOwnerLookup {
        private static boolean hasOwner(Mob mob) {
            for (String methodName : java.util.List.of("getOwnerUUID", "getOwnerId", "getOwner")) {
                try {
                    Object value = mob.getClass().getMethod(methodName).invoke(mob);
                    if (value != null) {
                        return true;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return false;
        }
    }
}
