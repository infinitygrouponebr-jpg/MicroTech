package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public interface ControlledMobBehavior {
    default boolean canKeepTarget(Mob mob, LivingEntity target, ServerPlayer controller) {
        return true;
    }

    default void tick(Mob mob, ServerPlayer controller) {
    }

    default void onTargetSelected(Mob mob, LivingEntity target, ServerPlayer controller) {
    }
}
