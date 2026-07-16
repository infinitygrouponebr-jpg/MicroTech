package Infinitygroup.microtech.entity.control;

import Infinitygroup.microtech.Config;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Slime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ControlledMobSupport {
    private static final int EFFECT_DURATION = 60;
    private static final Map<UUID, SupportWindow> SUPPORT_WINDOWS = new HashMap<>();

    private ControlledMobSupport() {
    }

    public static void tick(Mob mob, ServerPlayer controller) {
        if (!Config.enablePassiveMobBuffs || mob.tickCount % Config.passiveBuffRefreshInterval != 0 || mob.distanceToSqr(controller) > Config.passiveBuffRadius * Config.passiveBuffRadius) {
            return;
        }
        if (!ControlledMobCombatManager.isSupport(mob)) {
            return;
        }
        if (!claimSupportSlot(controller)) {
            return;
        }

        boolean applied = applyBuffFor(mob, controller);
        if (applied && mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, controller.getX(), controller.getY() + 1.0D, controller.getZ(), 2, 0.25D, 0.3D, 0.25D, 0.01D);
        }
    }

    private static boolean applyBuffFor(Mob mob, ServerPlayer controller) {
        boolean applied = false;
        if (mob instanceof Chicken && Config.chickenBuffEnabled) {
            applied |= addEffect(controller, MobEffects.SLOW_FALLING, 0);
        } else if (mob instanceof Pig && Config.pigBuffEnabled) {
            applied |= addEffect(controller, MobEffects.DAMAGE_RESISTANCE, 0);
        } else if (mob instanceof Cow && Config.cowBuffEnabled && controller.getHealth() < controller.getMaxHealth()) {
            applied |= addEffect(controller, MobEffects.REGENERATION, 0);
        } else if (mob instanceof Sheep && Config.sheepBuffEnabled) {
            applied |= addEffect(controller, MobEffects.ABSORPTION, 0);
        } else if (mob instanceof Rabbit && Config.rabbitBuffEnabled) {
            applied |= addEffect(controller, MobEffects.MOVEMENT_SPEED, 0);
            applied |= addEffect(controller, MobEffects.JUMP, 0);
        } else if (mob instanceof Slime slime && slime.getSize() <= 1 && Config.miniSlimeBuffEnabled) {
            applied |= addEffect(controller, MobEffects.JUMP, 1);
        } else if (mob instanceof Bee && Config.beeBuffEnabled && controller.getHealth() < controller.getMaxHealth()) {
            applied |= addEffect(controller, MobEffects.REGENERATION, 0);
        } else if (mob instanceof AbstractHorse && Config.horseBuffEnabled) {
            applied |= addEffect(controller, MobEffects.MOVEMENT_SPEED, 0);
        } else if (mob instanceof Turtle && Config.turtleBuffEnabled && controller.isInWaterOrBubble()) {
            applied |= addEffect(controller, MobEffects.WATER_BREATHING, 0);
            applied |= addEffect(controller, MobEffects.DAMAGE_RESISTANCE, 0);
        } else if (mob instanceof Axolotl && Config.axolotlBuffEnabled && controller.isInWaterOrBubble()) {
            applied |= addEffect(controller, MobEffects.REGENERATION, 0);
        } else if (mob instanceof net.minecraft.world.entity.ambient.Bat && Config.batBuffEnabled && isDarkFor(controller)) {
            applied |= addEffect(controller, MobEffects.NIGHT_VISION, 0);
        } else if (mob instanceof Fox && Config.foxBuffEnabled) {
            applied |= addEffect(controller, MobEffects.MOVEMENT_SPEED, 0);
            if (isDarkFor(controller)) {
                applied |= addEffect(controller, MobEffects.NIGHT_VISION, 0);
            }
        }
        return applied;
    }

    private static boolean addEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && existing.getAmplifier() > amplifier) {
            return false;
        }
        if (!Config.allowBuffStacking && existing != null && existing.getAmplifier() == amplifier && existing.getDuration() > EFFECT_DURATION / 2) {
            return false;
        }
        player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amplifier, true, false, true));
        return true;
    }

    private static boolean claimSupportSlot(ServerPlayer player) {
        long window = player.level().getGameTime() / Math.max(1, Config.passiveBuffRefreshInterval);
        SupportWindow current = SUPPORT_WINDOWS.get(player.getUUID());
        if (current == null || current.window() != window) {
            SUPPORT_WINDOWS.put(player.getUUID(), new SupportWindow(window, 1));
            return true;
        }
        if (current.count() >= Config.maxSupportMobsAffectingPlayer) {
            return false;
        }
        SUPPORT_WINDOWS.put(player.getUUID(), new SupportWindow(window, current.count() + 1));
        return true;
    }

    private static boolean isDarkFor(ServerPlayer player) {
        return player.level().isNight() || player.level().getMaxLocalRawBrightness(player.blockPosition()) <= 7;
    }

    private record SupportWindow(long window, int count) {
    }
}
