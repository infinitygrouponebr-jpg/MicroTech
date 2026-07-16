package Infinitygroup.microtech.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public final class TechSwordOverloadBeam {
    private static final int MIN_CHARGE_TICKS = 10;
    private static final int MAX_CHARGE_TICKS = 60;
    private static final int[] MIN_DAMAGE = {4, 6, 8, 10, 13};
    private static final int[] MAX_DAMAGE = {10, 14, 18, 23, 30};
    private static final double[] MIN_RANGE = {8.0D, 10.0D, 12.0D, 14.0D, 16.0D};
    private static final double[] MAX_RANGE = {14.0D, 18.0D, 22.0D, 26.0D, 30.0D};
    private static final int[] MIN_VISUAL_TICKS = {6, 8, 10, 12, 14};
    private static final int[] MAX_VISUAL_TICKS = {14, 16, 18, 20, 24};

    private static final Map<UUID, BeamEffect> ACTIVE_BEAMS = new HashMap<>();

    private TechSwordOverloadBeam() {
    }

    public static int getMinChargeTicks() {
        return MIN_CHARGE_TICKS;
    }

    public static int getMaxChargeTicks() {
        return MAX_CHARGE_TICKS;
    }

    public static int getMinimumEnergyToStart(int level) {
        return getPerTickEnergyCost(level) * MIN_CHARGE_TICKS;
    }

    public static int getPerTickEnergyCost(int level) {
        return Math.max(1, TechChipType.OVERLOAD.getEnergyCost(level) / 20);
    }

    public static float getChargePercent(int chargeTicks) {
        if (chargeTicks <= 0) {
            return 0.0F;
        }
        return Mth.clamp(chargeTicks / (float) MAX_CHARGE_TICKS, 0.0F, 1.0F);
    }

    public static float getDamage(int level, int chargeTicks) {
        float progress = getChargePercent(chargeTicks);
        return Mth.lerp(progress, MIN_DAMAGE[level - 1], MAX_DAMAGE[level - 1]);
    }

    public static double getRange(int level, int chargeTicks) {
        float progress = getChargePercent(chargeTicks);
        return Mth.lerp(progress, MIN_RANGE[level - 1], MAX_RANGE[level - 1]);
    }

    public static int getVisualTicks(int level, int chargeTicks) {
        float progress = getChargePercent(chargeTicks);
        return Mth.clamp(Math.round(Mth.lerp(progress, MIN_VISUAL_TICKS[level - 1], MAX_VISUAL_TICKS[level - 1])), 1, MAX_VISUAL_TICKS[level - 1]);
    }

    public static int getMaxTargets(int level) {
        return Mth.clamp(level + 1, 2, 6);
    }

    public static void startBeam(ServerLevel level, Player player, Vec3 start, Vec3 end, int visualTicks, float intensity) {
        if (visualTicks <= 0) {
            return;
        }

        ACTIVE_BEAMS.put(player.getUUID(), new BeamEffect(level.dimension(), start, end, visualTicks, Mth.clamp(intensity, 0.0F, 1.0F)));
        spawnBeamPulse(level, start, end, intensity);
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE_BEAMS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, BeamEffect>> iterator = ACTIVE_BEAMS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BeamEffect> entry = iterator.next();
            BeamEffect beam = entry.getValue();
            ServerLevel level = server.getLevel(beam.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }

            spawnBeamPulse(level, beam.start(), beam.end(), beam.intensity());
            beam.ticksRemaining--;
            if (beam.ticksRemaining <= 0) {
                iterator.remove();
            }
        }
    }

    public static void clear(UUID playerId) {
        ACTIVE_BEAMS.remove(playerId);
    }

    private static void spawnBeamPulse(ServerLevel level, Vec3 start, Vec3 end, float intensity) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 0.05D) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.2F, 0.82F, 1.0F), 1.0F), start.x, start.y, start.z, 2, 0.05D, 0.05D, 0.05D, 0.0D);
            return;
        }

        int steps = Mth.clamp((int) Math.ceil(length / 0.45D), 6, 32);
        int sparkCount = 1 + Math.round(intensity * 2.0F);
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            double x = Mth.lerp(progress, start.x, end.x);
            double y = Mth.lerp(progress, start.y, end.y);
            double z = Mth.lerp(progress, start.z, end.z);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, sparkCount, 0.03D, 0.03D, 0.03D, 0.0D);
            if (i % 2 == 0) {
                level.sendParticles(new DustParticleOptions(new Vector3f(0.16F, 0.75F, 1.0F), 1.0F), x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }

        level.sendParticles(new DustParticleOptions(new Vector3f(0.9F, 0.98F, 1.0F), 1.0F), end.x, end.y, end.z, 3 + Math.round(intensity * 3.0F), 0.08D, 0.08D, 0.08D, 0.0D);
    }

    private static final class BeamEffect {
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final Vec3 start;
        private final Vec3 end;
        private int ticksRemaining;
        private final float intensity;

        private BeamEffect(net.minecraft.resources.ResourceKey<Level> dimension, Vec3 start, Vec3 end, int ticksRemaining, float intensity) {
            this.dimension = dimension;
            this.start = start;
            this.end = end;
            this.ticksRemaining = ticksRemaining;
            this.intensity = intensity;
        }

        private net.minecraft.resources.ResourceKey<Level> dimension() {
            return this.dimension;
        }

        private Vec3 start() {
            return this.start;
        }

        private Vec3 end() {
            return this.end;
        }

        private float intensity() {
            return this.intensity;
        }
    }
}
