package Infinitygroup.microtech.item;

public enum SwordEvolutionTier {
    TIER_1(1, 10, 5, 100_000, 250, 1_000);

    private final int id;
    private final int chargedDamage;
    private final int unchargedDamage;
    private final int energyCapacity;
    private final int energyCost;
    private final int maxReceive;

    SwordEvolutionTier(int id, int chargedDamage, int unchargedDamage, int energyCapacity, int energyCost, int maxReceive) {
        this.id = id;
        this.chargedDamage = chargedDamage;
        this.unchargedDamage = unchargedDamage;
        this.energyCapacity = energyCapacity;
        this.energyCost = energyCost;
        this.maxReceive = maxReceive;
    }

    public int getId() {
        return this.id;
    }

    public int getChargedDamage() {
        return this.chargedDamage;
    }

    public int getUnchargedDamage() {
        return this.unchargedDamage;
    }

    public int getEnergyCapacity() {
        return this.energyCapacity;
    }

    public int getEnergyCost() {
        return this.energyCost;
    }

    public int getMaxReceive() {
        return this.maxReceive;
    }

    public static SwordEvolutionTier fromId(int id) {
        for (SwordEvolutionTier tier : values()) {
            if (tier.id == id) {
                return tier;
            }
        }

        return TIER_1;
    }
}
