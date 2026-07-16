package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class ControlledWardenBossBehavior implements ControlledBossBehavior {
    public static final ControlledWardenBossBehavior INSTANCE = new ControlledWardenBossBehavior();

    private ControlledWardenBossBehavior() {
    }

    @Override
    public void tick(Mob boss, ServerPlayer controller) {
        WardenControlledBehavior.INSTANCE.tick(boss, controller);
    }

    @Override
    public void onTargetSelected(Mob boss, LivingEntity target, ServerPlayer controller) {
        WardenControlledBehavior.INSTANCE.onTargetSelected(boss, target, controller);
    }
}
