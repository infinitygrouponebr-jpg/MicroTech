package Infinitygroup.microtech.entity.control;

import Infinitygroup.microtech.Config;
import Infinitygroup.microtech.Microtech;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public final class ControlledMobEvents {
    private ControlledMobEvents() {
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        ControlledTemporaryEntityTracker.registerIfControlled(event.getEntity());
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (ControlledMobData.isControlled(mob)) {
            ControlledMobManager.installGoals(mob);
        }
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        ControlledTemporaryEntityTracker.unregister(event.getEntity());
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        ControlledMobManager.onMobUnloaded(mob);
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        Entity source = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();

        Entity controlledSource = findControlledDamageSource(direct, source);
        if (controlledSource != null && !ControlledMobAllianceService.canControlledMobDamage(controlledSource, event.getEntity())) {
            event.setNewDamage(0.0F);
            if (source instanceof Mob mob && ControlledMobAllianceService.isProtectedAlly(mob, event.getEntity())) {
                mob.setTarget(null);
            }
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        ControlledMobManager.rememberDamage(event.getEntity(), attacker, event.getEntity().level().getGameTime());

        if (!(event.getEntity().level() instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (attacker instanceof ServerPlayer controller) {
            ControlledMobCommandTargetService.rememberControllerDamage(controller, victim, "controller_damage");
        }
        if (victim instanceof ServerPlayer controller && attacker instanceof LivingEntity livingAttacker) {
            ControlledMobCommandTargetService.rememberControllerAttacker(controller, livingAttacker, "controller_attacked");
        }
        ControlledMobAllianceService.resolveController(victim).ifPresent(controller ->
                {
                    if (attacker instanceof LivingEntity livingAttacker) {
                        ControlledMobCommandTargetService.rememberControlledAllyAttacker(controller, serverLevel, livingAttacker, "controlled_ally_attacked");
                    }
                }
        );
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob) || !ControlledMobData.isControlled(mob)) {
            return;
        }
        if (mob.level() instanceof ServerLevel serverLevel) {
            if (ControlledMobData.getTier(mob) == ControlledMobTier.ADVANCED && ControlledMobManager.isBossLike(mob)) {
                if (Config.advancedChipDropsOnBossDeath) {
                    mob.spawnAtLocation(new ItemStack(Microtech.ADVANCED_CONTROLLER_CHIP.get()));
                }
            } else if (Config.controllerChipDropsOnControlledMobDeath) {
                mob.spawnAtLocation(new ItemStack(Microtech.CONTROLLER_CHIP.get()));
            }
        }
        ControlledMobManager.onControlledMobRemoved(mob);
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.getHand() != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.is(Microtech.ADVANCED_CONTROLLER_CHIP.get()) && event.getTarget() instanceof Mob targetMob) {
            if (player.isShiftKeyDown()) {
                if (ControlledMobManager.removeChip(targetMob, serverPlayer)) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
                return;
            }
            ControlledMobManager.ApplyResult result = ControlledMobManager.tryInstallAdvanced(targetMob, serverPlayer, held, event.getHand());
            if (!result.success()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(result.messageKey(), result.args()), true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (!(event.getTarget() instanceof Mob mob) || !ControlledMobData.isControlledBy(mob, player.getUUID())) {
            return;
        }
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return;
        }

        ControlledMobOrder order;
        if (player.isShiftKeyDown()) {
            ControlledMobData.setGuard(mob, mob.blockPosition(), mob.level().dimension());
            order = ControlledMobOrder.GUARD;
        } else {
            ControlledMobOrder current = ControlledMobData.getOrder(mob);
            order = current == ControlledMobOrder.FOLLOW ? ControlledMobOrder.STAY : ControlledMobOrder.FOLLOW;
            if (order == ControlledMobOrder.STAY) {
                ControlledMobData.setStay(mob, mob.blockPosition(), mob.level().dimension());
            } else {
                ControlledMobData.setOrder(mob, order);
            }
        }
        player.displayClientMessage(ComponentHelper.order(order), true);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        Projectile projectile = event.getProjectile();
        if (projectile.getOwner() instanceof ServerPlayer controller && hit.getEntity() instanceof LivingEntity target) {
            ControlledMobCommandTargetService.rememberControllerDamage(controller, target, "controller_projectile");
        }
        ControlledMobManager.getControlledBossSource(projectile).ifPresent(boss -> {
            if (hit.getEntity() instanceof LivingEntity target && !ControlledMobAllianceService.canControlledMobDamage(projectile, target)) {
                event.setCanceled(true);
            }
        });
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        Entity source = event.getExplosion().getDirectSourceEntity();
        ControlledMobManager.getControlledBossSource(source).ifPresent(boss -> {
            boolean blockGrief = Config.controlledBossBlockGriefing
                    || (boss instanceof net.minecraft.world.entity.boss.wither.WitherBoss && Config.controlledWitherBlockGriefing)
                    || (boss instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon && Config.controlledDragonBlockGriefing);
            if (!blockGrief) {
                event.getAffectedBlocks().clear();
            }
            if (!ControlledMobAllianceService.isFriendlyFireAllowed(source)) {
                event.getAffectedEntities().removeIf(entity -> entity instanceof LivingEntity living && ControlledMobAllianceService.isProtectedAlly(boss, living));
            }
        });
    }

    public static void onEntityMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.boss.wither.WitherBoss wither
                && ControlledMobManager.isAdvancedControlledBoss(wither)
                && !Config.controlledWitherBlockGriefing) {
            event.setCanGrief(false);
            return;
        }
        if (event.getEntity() instanceof Mob mob && ControlledMobManager.isAdvancedControlledBoss(mob) && !Config.controlledBossBlockGriefing) {
            event.setCanGrief(false);
        }
    }

    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity().level().isClientSide || event.getEffectInstance() == null) {
            return;
        }
        Entity source = event.getEffectSource();
        if (event.getEffectInstance().is(MobEffects.DIG_SLOWDOWN)
                && source instanceof ElderGuardian guardian
                && ControlledMobManager.isAdvancedControlledBoss(guardian)
                && ControlledMobAllianceService.isProtectedAlly(guardian, event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (event.getEffectInstance().is(MobEffects.DARKNESS)
                && shouldBlockControlledWardenDarkness(source, event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (event.getEffectInstance().is(MobEffects.WITHER)
                && source != null
                && !ControlledMobAllianceService.canControlledMobDamage(source, event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob) || !ControlledMobData.isControlled(mob)) {
            return;
        }

        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget != null && !ControlledMobAllianceService.canControlledMobTarget(mob, newTarget)) {
            event.setCanceled(true);
        }
    }

    private static Entity findControlledDamageSource(Entity direct, Entity source) {
        if (direct != null && (ControlledMobAllianceService.resolveController(direct).isPresent()
                || ControlledTemporaryEntityTracker.resolveCreator(direct).filter(ControlledMobData::isControlled).isPresent())) {
            return direct;
        }
        if (source != null && ControlledMobAllianceService.resolveController(source).isPresent()) {
            return source;
        }
        return null;
    }

    private static boolean shouldBlockControlledWardenDarkness(Entity source, LivingEntity target) {
        if (source instanceof net.minecraft.world.entity.monster.warden.Warden warden
                && ControlledMobManager.isAdvancedControlledBoss(warden)
                && ControlledMobAllianceService.isProtectedAlly(warden, target)) {
            return true;
        }
        return source == null
                && ControlledWardenDarknessTracker.wasRecentlyEmittedByControlledWardenAtProtectedTarget(target);
    }
}
