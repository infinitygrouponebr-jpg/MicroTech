package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public final class ControlledWitherBehavior implements ControlledBossBehavior {
    public static final ControlledWitherBehavior INSTANCE = new ControlledWitherBehavior();

    private ControlledWitherBehavior() {
    }

    @Override
    public void tick(Mob boss, ServerPlayer controller) {
        // The target selection hook fills the Wither's head targets. Clearing
        // them here would cancel the vanilla skull attack before it can fire.
    }

    @Override
    public void onTargetSelected(Mob boss, LivingEntity target, ServerPlayer controller) {
        if (boss instanceof WitherBoss wither) {
            int id = target.getId();
            wither.setAlternativeTarget(0, id);
            wither.setAlternativeTarget(1, id);
            wither.setAlternativeTarget(2, id);
        }
    }
}
