package Infinitygroup.microtech.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class TechArmorFlightHandler {
    private static final String FLIGHT_MESSAGE_KEY = "message.microtech.tech_armor.flight_low_power";
    private static final int FLIGHT_COST_PER_TICK = 20;
    private static final long LOW_POWER_MESSAGE_INTERVAL_TICKS = 40L;
    private static final Map<UUID, Long> LAST_LOW_POWER_MESSAGE = new HashMap<>();

    private TechArmorFlightHandler() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_LOW_POWER_MESSAGE.remove(event.getEntity().getUUID());
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        LAST_LOW_POWER_MESSAGE.remove(event.getOriginal().getUUID());
    }

    private static void tickPlayer(ServerPlayer player) {
        if (player.isSpectator() || player.getAbilities().instabuild) {
            return;
        }

        if (!canUseFlight(player)) {
            disableFlight(player);
            return;
        }

        boolean changed = false;
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            changed = true;
        }

        player.getAbilities().mayfly = true;

        if (!player.getAbilities().flying) {
            if (changed) {
                player.onUpdateAbilities();
            }
            return;
        }

        int consumed = TechArmorEnergyHelper.consumeEnergyFromChestplate(player, FLIGHT_COST_PER_TICK);
        if (consumed < FLIGHT_COST_PER_TICK) {
            disableFlight(player);
            warnLowPower(player);
            return;
        }

        if (changed) {
            player.onUpdateAbilities();
        }
    }

    private static boolean canUseFlight(Player player) {
        return TechArmorEnergyHelper.isFullTechArmorSet(player)
                && TechArmorFlightVisualHelper.hasFlightChipInChestplate(player)
                && TechArmorFlightVisualHelper.hasChestplateEnergyForFlight(player);
    }

    private static void disableFlight(ServerPlayer player) {
        if (!player.getAbilities().instabuild && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
    }

    private static void warnLowPower(ServerPlayer player) {
        long now = player.level().getGameTime();
        long last = LAST_LOW_POWER_MESSAGE.getOrDefault(player.getUUID(), 0L);
        if (now - last < LOW_POWER_MESSAGE_INTERVAL_TICKS) {
            return;
        }

        LAST_LOW_POWER_MESSAGE.put(player.getUUID(), now);
        player.displayClientMessage(Component.translatable(FLIGHT_MESSAGE_KEY), true);
    }
}
