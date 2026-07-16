package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public interface ControlledBossBehavior {
    default void onInstalled(Mob boss, ServerPlayer controller) {
    }

    default void onRemoved(Mob boss, ServerPlayer controller) {
    }

    default void tick(Mob boss, ServerPlayer controller) {
    }

    default void onTargetSelected(Mob boss, LivingEntity target, ServerPlayer controller) {
    }
}
