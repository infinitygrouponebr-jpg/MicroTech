package Infinitygroup.microtech.item;

import java.util.Arrays;
import java.util.List;

public enum TechChipType {
    ENERGY_CUT(
            "energy_cut",
            5,
            false,
            new int[] {50, 120, 220, 360, 550},
            new int[] {1, 2, 3, 4, 5},
            new double[] {0.0D, 0.0D, 0.0D, 0.0D, 0.0D},
            new double[] {0.0D, 0.0D, 0.0D, 0.0D, 0.0D},
            new int[] {0, 0, 0, 0, 0},
            new int[] {0, 0, 0, 0, 0}
    ),
    SHOCK_DISCHARGE(
            "shock_discharge",
            5,
            false,
            new int[] {300, 500, 800, 1200, 1700},
            new int[] {2, 3, 4, 5, 6},
            new double[] {1.5D, 2.0D, 2.5D, 3.0D, 3.5D},
            new double[] {0.35D, 0.45D, 0.55D, 0.70D, 0.85D},
            new int[] {0, 0, 0, 0, 0},
            new int[] {0, 0, 0, 0, 0}
    ),
    OVERLOAD(
            "overload",
            5,
            true,
            new int[] {600, 900, 1300, 1800, 2500},
            new int[] {0, 0, 0, 0, 0},
            new double[] {0.0D, 0.0D, 0.0D, 0.0D, 0.0D},
            new double[] {0.0D, 0.0D, 0.0D, 0.0D, 0.0D},
            new int[] {120, 140, 160, 180, 200},
            new int[] {0, 0, 0, 0, 0}
    );

    private final String id;
    private final int maxLevel;
    private final boolean active;
    private final int[] energyCosts;
    private final int[] damageBonuses;
    private final double[] radii;
    private final double[] damagePercents;
    private final int[] cooldownTicks;
    private final int[] armedDurationTicks;

    TechChipType(String id, int maxLevel, boolean active, int[] energyCosts, int[] damageBonuses, double[] radii, double[] damagePercents, int[] cooldownTicks, int[] armedDurationTicks) {
        this.id = id;
        this.maxLevel = maxLevel;
        this.active = active;
        this.energyCosts = energyCosts;
        this.damageBonuses = damageBonuses;
        this.radii = radii;
        this.damagePercents = damagePercents;
        this.cooldownTicks = cooldownTicks;
        this.armedDurationTicks = armedDurationTicks;
    }

    public String getId() {
        return this.id;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public boolean isActive() {
        return this.active;
    }

    public int getEnergyCost(int level) {
        if (level < 1 || level > this.maxLevel) {
            return 0;
        }
        return this.energyCosts[level - 1];
    }

    public int getDamageBonus(int level) {
        if (level < 1 || level > this.maxLevel) {
            return 0;
        }
        return this.damageBonuses[level - 1];
    }

    public double getRadius(int level) {
        if (level < 1 || level > this.maxLevel) {
            return 0.0D;
        }
        return this.radii[level - 1];
    }

    public double getDamagePercent(int level) {
        if (level < 1 || level > this.maxLevel) {
            return 0.0D;
        }
        return this.damagePercents[level - 1];
    }

    public int getCooldownTicks(int level) {
        if (level < 1 || level > this.maxLevel) {
            return 0;
        }
        return this.cooldownTicks[level - 1];
    }

    public int getArmedDurationTicks(int level) {
        if (level < 1 || level > this.maxLevel) {
            return 0;
        }
        return this.armedDurationTicks[level - 1];
    }

    public static List<TechChipType> activeTypes() {
        return Arrays.stream(values())
                .filter(TechChipType::isActive)
                .toList();
    }
}
