package Infinitygroup.microtech.entity.control;

public enum ControlledMobTier {
    BASIC,
    ADVANCED;

    public static ControlledMobTier byId(String id) {
        for (ControlledMobTier tier : values()) {
            if (tier.name().equalsIgnoreCase(id)) {
                return tier;
            }
        }
        return BASIC;
    }
}
