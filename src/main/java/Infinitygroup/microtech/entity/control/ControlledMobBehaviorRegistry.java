package Infinitygroup.microtech.entity.control;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;

public final class ControlledMobBehaviorRegistry {
    private ControlledMobBehaviorRegistry() {
    }

    public static ControlledMobBehavior get(Mob mob) {
        if (mob instanceof Creeper) {
            return CreeperControlledBehavior.INSTANCE;
        }
        if (mob instanceof Warden) {
            return WardenControlledBehavior.INSTANCE;
        }
        return DefaultControlledMobBehavior.INSTANCE;
    }
}
