package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;

import java.util.Optional;

public final class WardenControlledBehavior implements ControlledMobBehavior {
    public static final WardenControlledBehavior INSTANCE = new WardenControlledBehavior();

    private WardenControlledBehavior() {
    }

    @Override
    public void tick(Mob mob, ServerPlayer controller) {
        if (mob instanceof Warden warden) {
            if ((warden.tickCount + warden.getId()) % 120 == 0) {
                ControlledWardenDarknessTracker.recordPulse(warden, controller, 20);
            }
            warden.clearAnger(controller);
            cleanProtectedHostileMemory(warden, controller, MemoryModuleType.ATTACK_TARGET);
            cleanProtectedHostileMemory(warden, controller, MemoryModuleType.ROAR_TARGET);
            cleanProtectedHostileMemory(warden, controller, MemoryModuleType.NEAREST_ATTACKABLE);

            ControlledMobCommandTargetService.getCommandedTarget(warden, controller)
                    .ifPresent(target -> applyAttackTarget(warden, target, controller));
        }
    }

    @Override
    public void onTargetSelected(Mob mob, LivingEntity target, ServerPlayer controller) {
        if (mob instanceof Warden warden) {
            applyAttackTarget(warden, target, controller);
        }
    }

    private void cleanProtectedHostileMemory(Warden warden, ServerPlayer controller, MemoryModuleType<? extends LivingEntity> memory) {
        Brain<Warden> brain = warden.getBrain();
        Optional<? extends LivingEntity> remembered = brain.getMemory(memory);
        remembered.ifPresent(target -> {
            if (ControlledMobAllianceService.isProtectedAlly(warden, target)) {
                warden.clearAnger(target);
                brain.eraseMemory(memory);
                if (warden.getTarget() == target) {
                    warden.setAttackTarget(null);
                }
            }
        });
        warden.clearAnger(controller);
    }

    private void applyAttackTarget(Warden warden, LivingEntity target, ServerPlayer controller) {
        warden.clearAnger(controller);
        if (!ControlledMobAllianceService.canControlledMobTarget(warden, target)) {
            warden.clearAnger(target);
            if (warden.getTarget() == target) {
                warden.setAttackTarget(null);
            }
            return;
        }

        if (warden.getTarget() != target) {
            warden.increaseAngerAt(target, 80, false);
            warden.setAttackTarget(target);
        }
    }
}
