package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public final class ControlledWitherBehavior implements ControlledBossBehavior {
    public static final ControlledWitherBehavior INSTANCE = new ControlledWitherBehavior();

    private ControlledWitherBehavior() {
    }

    @Override
    public void tick(Mob boss, ServerPlayer controller) {
        if (!(boss instanceof WitherBoss wither) || !(wither.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity commanded = ControlledMobCommandTargetService.getCommandedTarget(wither, controller).orElse(null);
        if (commanded != null) {
            applyTarget(wither, commanded);
            return;
        }

        LivingEntity mainTarget = wither.getTarget();
        if (mainTarget != null && !ControlledMobAllianceService.canControlledMobTarget(wither, mainTarget)) {
            wither.setTarget(null);
            wither.setAlternativeTarget(0, 0);
        }

        for (int head = 0; head < 3; head++) {
            int id = wither.getAlternativeTarget(head);
            if (id <= 0) {
                continue;
            }

            Entity entity = serverLevel.getEntity(id);
            if (!(entity instanceof LivingEntity target) || !ControlledMobAllianceService.canControlledMobTarget(wither, target)) {
                wither.setAlternativeTarget(head, 0);
            }
        }
    }

    @Override
    public void onTargetSelected(Mob boss, LivingEntity target, ServerPlayer controller) {
        if (boss instanceof WitherBoss wither) {
            if (ControlledMobAllianceService.canControlledMobTarget(wither, target)) {
                applyTarget(wither, target);
            }
        }
    }

    private void applyTarget(WitherBoss wither, LivingEntity target) {
        if (wither.getTarget() != target) {
            wither.setTarget(target);
        }
        int id = target.getId();
        for (int head = 0; head < 3; head++) {
            if (wither.getAlternativeTarget(head) != id) {
                wither.setAlternativeTarget(head, id);
            }
        }
    }
}
