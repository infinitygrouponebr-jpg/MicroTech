package Infinitygroup.microtech.client.hud;

import Infinitygroup.microtech.item.TechArmorEnergyHelper;
import Infinitygroup.microtech.item.TechArmorFlightVisualHelper;
import Infinitygroup.microtech.item.TechArmorUpgradeHelper;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.biome.Biome;

public record TechArmorHudData(
        int chargePercent,
        int energyCurrent,
        int energyMax,
        String status,
        int helmetPercent,
        int chestPercent,
        int legsPercent,
        int bootsPercent,
        float healthCurrent,
        float healthMax,
        String coords,
        String direction,
        String biome,
        String flightStatus
) {
    private static final int FLIGHT_COST_PER_TICK = 20;

    public static TechArmorHudData collect(LocalPlayer player) {
        int totalCurrent = TechArmorEnergyHelper.getTotalEnergy(player);
        int totalMax = TechArmorEnergyHelper.getTotalMaxEnergy(player);
        int chargePercent = totalMax <= 0 ? 0 : Math.max(0, Math.min(100, Math.round(totalCurrent * 100.0F / totalMax)));

        BlockPos pos = player.blockPosition();
        String coords = String.format(Locale.ROOT, "X %d  Y %d  Z %d", pos.getX(), pos.getY(), pos.getZ());
        String direction = player.getDirection().getName().toUpperCase(Locale.ROOT);

        String biome = "unknown";
        Holder<Biome> biomeHolder = player.level().getBiome(pos);
        if (biomeHolder.unwrapKey().isPresent()) {
            ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().get();
            biome = biomeKey.location().getPath().replace('_', ' ');
        }

        return new TechArmorHudData(
                chargePercent,
                totalCurrent,
                totalMax,
                TechArmorEnergyHelper.getStatus(player),
                TechArmorEnergyHelper.getEnergyPercent(player.getItemBySlot(EquipmentSlot.HEAD)),
                TechArmorEnergyHelper.getEnergyPercent(player.getItemBySlot(EquipmentSlot.CHEST)),
                TechArmorEnergyHelper.getEnergyPercent(player.getItemBySlot(EquipmentSlot.LEGS)),
                TechArmorEnergyHelper.getEnergyPercent(player.getItemBySlot(EquipmentSlot.FEET)),
                player.getHealth(),
                player.getMaxHealth(),
                coords,
                direction,
                biome,
                getFlightStatus(player)
        );
    }

    public String energyLine() {
        return "ENERGY " + this.energyCurrent + " / " + this.energyMax + " FE";
    }

    public String flightLine() {
        return "FLIGHT: " + this.flightStatus;
    }

    private static String getFlightStatus(LocalPlayer player) {
        if (!TechArmorEnergyHelper.isFullTechArmorSet(player)) {
            return "NO CHIP";
        }

        if (!TechArmorUpgradeHelper.hasFlightChip(TechArmorEnergyHelper.getChestplate(player))) {
            return "NO CHIP";
        }

        int chestEnergy = TechArmorEnergyHelper.getChestplateEnergy(player);
        if (chestEnergy <= 0) {
            return "OFFLINE";
        }

        if (TechArmorFlightVisualHelper.isTechFlightActive(player)) {
            return "ACTIVE";
        }

        if (chestEnergy < FLIGHT_COST_PER_TICK) {
            return "LOW POWER";
        }

        return "READY";
    }
}
