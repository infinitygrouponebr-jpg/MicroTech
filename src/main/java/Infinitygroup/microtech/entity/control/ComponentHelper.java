package Infinitygroup.microtech.entity.control;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;

public final class ComponentHelper {
    private ComponentHelper() {
    }

    public static Component installed(Mob mob) {
        return Component.translatable("message.microtech.controller_chip.installed", mob.getDisplayName());
    }

    public static Component advancedInstalled(Mob mob) {
        return Component.translatable("message.microtech.advanced_controller_chip.installed", mob.getDisplayName());
    }

    public static Component removed(Mob mob) {
        return Component.translatable("message.microtech.controller_chip.removed", mob.getDisplayName());
    }

    public static Component notController() {
        return Component.translatable("message.microtech.controller_chip.not_controller");
    }

    public static Component order(ControlledMobOrder order) {
        return Component.translatable("message.microtech.controller_chip.order", order.getDisplayName());
    }
}
