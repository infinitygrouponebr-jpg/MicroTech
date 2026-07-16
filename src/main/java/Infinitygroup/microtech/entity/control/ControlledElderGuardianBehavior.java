package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ElderGuardian;

public final class ControlledElderGuardianBehavior implements ControlledBossBehavior {
    public static final ControlledElderGuardianBehavior INSTANCE = new ControlledElderGuardianBehavior();

    private ControlledElderGuardianBehavior() {
    }

    @Override
    public void tick(Mob boss, ServerPlayer controller) {
        if (boss instanceof ElderGuardian && !boss.isInWaterOrBubble()) {
            boss.getNavigation().stop();
            ControlledMobData.setOrder(boss, ControlledMobOrder.STAY);
        }
    }
}
