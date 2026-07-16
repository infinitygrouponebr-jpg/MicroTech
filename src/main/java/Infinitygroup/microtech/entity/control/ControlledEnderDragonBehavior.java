package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

public final class ControlledEnderDragonBehavior implements ControlledBossBehavior {
    public static final ControlledEnderDragonBehavior INSTANCE = new ControlledEnderDragonBehavior();

    private ControlledEnderDragonBehavior() {
    }

    @Override
    public void tick(Mob boss, ServerPlayer controller) {
        if (boss instanceof EnderDragon dragon && dragon.getTarget() == null && dragon.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.HOLDING_PATTERN) {
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        }
    }

    @Override
    public void onTargetSelected(Mob boss, LivingEntity target, ServerPlayer controller) {
        if (boss instanceof EnderDragon dragon) {
            DragonChargePlayerPhase phase = dragon.getPhaseManager().getPhase(EnderDragonPhase.CHARGING_PLAYER);
            phase.setTarget(target.position());
            dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
        }
    }
}
