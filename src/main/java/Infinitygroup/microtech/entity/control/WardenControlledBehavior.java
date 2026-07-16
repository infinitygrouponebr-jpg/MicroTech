package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;

public final class WardenControlledBehavior implements ControlledMobBehavior {
    public static final WardenControlledBehavior INSTANCE = new WardenControlledBehavior();

    private WardenControlledBehavior() {
    }

    @Override
    public void tick(Mob mob, ServerPlayer controller) {
        if (mob instanceof Warden warden) {
            warden.clearAnger(controller);
            LivingEntity target = warden.getTarget();
            if (target != null && ControlledMobManager.areAllies(controller, target)) {
                warden.clearAnger(target);
                warden.setAttackTarget(null);
            }
        }
    }

    @Override
    public void onTargetSelected(Mob mob, LivingEntity target, ServerPlayer controller) {
        if (mob instanceof Warden warden) {
            warden.clearAnger(controller);
            warden.increaseAngerAt(target, 80, false);
            warden.setAttackTarget(target);
        }
    }
}
