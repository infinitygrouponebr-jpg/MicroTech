package Infinitygroup.microtech.entity.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;

public final class CreeperControlledBehavior implements ControlledMobBehavior {
    public static final CreeperControlledBehavior INSTANCE = new CreeperControlledBehavior();

    private CreeperControlledBehavior() {
    }

    @Override
    public boolean canKeepTarget(Mob mob, LivingEntity target, ServerPlayer controller) {
        return mob.distanceToSqr(controller) > 36.0D;
    }

    @Override
    public void tick(Mob mob, ServerPlayer controller) {
        if (mob instanceof Creeper creeper && creeper.distanceToSqr(controller) < 36.0D) {
            creeper.setSwellDir(-1);
        }
    }
}
