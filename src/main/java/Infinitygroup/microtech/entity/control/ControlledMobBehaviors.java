package Infinitygroup.microtech.entity.control;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;

public final class ControlledMobBehaviors {
    private ControlledMobBehaviors() {
    }

    public static ControlledMobBehavior get(Mob mob) {
        return ControlledMobBehaviorRegistry.get(mob);
    }
}
