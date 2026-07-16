package Infinitygroup.microtech.client;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.item.TechArmorFlightVisualHelper;
import Infinitygroup.microtech.item.TechArmorFlightVisualHelper.FlightVisualState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Microtech.MODID, value = Dist.CLIENT)
public final class TechArmorFlightClientEvents {
    private static final int GLIDE_PARTICLE_INTERVAL_TICKS = 4;
    private static final int HOVER_PARTICLE_INTERVAL_TICKS = 8;

    private TechArmorFlightClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        for (Player player : minecraft.level.players()) {
            FlightVisualState state = TechArmorFlightVisualHelper.getFlightVisualState(player);
            if (state == FlightVisualState.FLIGHT_NONE) {
                continue;
            }

            if (state == FlightVisualState.FLIGHT_GLIDE && (player.tickCount % GLIDE_PARTICLE_INTERVAL_TICKS) == 0) {
                spawnGlideParticles(minecraft, player);
            } else if (state == FlightVisualState.FLIGHT_HOVER && (player.tickCount % HOVER_PARTICLE_INTERVAL_TICKS) == 0) {
                spawnHoverParticles(minecraft, player);
            }
        }
    }

    private static void spawnGlideParticles(Minecraft minecraft, Player player) {
        if (minecraft.level == null) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        Vec3 trailDirection = movement.lengthSqr() > 1.0E-4D ? movement.normalize().scale(-0.55D) : player.getLookAngle().scale(-0.55D);
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.72D, 0.0D).add(trailDirection);
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.35F, 0.82F, 1.0F), 0.85F);

        for (int i = 0; i < 2; i++) {
            double offsetX = (player.getRandom().nextDouble() - 0.5D) * 0.16D;
            double offsetY = (player.getRandom().nextDouble() - 0.5D) * 0.16D;
            double offsetZ = (player.getRandom().nextDouble() - 0.5D) * 0.16D;
            minecraft.level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    origin.x + offsetX,
                    origin.y + offsetY,
                    origin.z + offsetZ,
                    trailDirection.x * 0.04D,
                    trailDirection.y * 0.04D,
                    trailDirection.z * 0.04D
            );
        }

        minecraft.level.addParticle(
                ParticleTypes.END_ROD,
                origin.x,
                origin.y + 0.05D,
                origin.z,
                trailDirection.x * 0.03D,
                trailDirection.y * 0.03D,
                trailDirection.z * 0.03D
        );

        if ((player.tickCount & 7) == 0) {
            minecraft.level.addParticle(dust, origin.x, origin.y - 0.05D, origin.z, -look.x * 0.01D, -look.y * 0.01D, -look.z * 0.01D);
        }
    }

    private static void spawnHoverParticles(Minecraft minecraft, Player player) {
        if (minecraft.level == null) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.68D, 0.0D).subtract(look.scale(0.35D));

        minecraft.level.addParticle(
                ParticleTypes.ELECTRIC_SPARK,
                origin.x,
                origin.y - 0.06D,
                origin.z,
                -look.x * 0.02D,
                -0.02D,
                -look.z * 0.02D
        );

        if ((player.tickCount & 7) == 0) {
            minecraft.level.addParticle(
                    ParticleTypes.END_ROD,
                    origin.x,
                    origin.y - 0.02D,
                    origin.z,
                    0.0D,
                    -0.01D,
                    0.0D
            );
        }
    }
}
