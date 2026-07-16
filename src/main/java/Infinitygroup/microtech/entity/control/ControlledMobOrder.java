package Infinitygroup.microtech.entity.control;

import net.minecraft.network.chat.Component;

public enum ControlledMobOrder {
    FOLLOW("follow", "message.microtech.controller_chip.order_follow"),
    STAY("stay", "message.microtech.controller_chip.order_stay"),
    GUARD("guard", "message.microtech.controller_chip.order_guard");

    private final String id;
    private final String translationKey;

    ControlledMobOrder(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String getId() {
        return this.id;
    }

    public Component getDisplayName() {
        return Component.translatable(this.translationKey);
    }

    public static ControlledMobOrder byId(String id) {
        for (ControlledMobOrder order : values()) {
            if (order.id.equals(id)) {
                return order;
            }
        }
        return FOLLOW;
    }
}
