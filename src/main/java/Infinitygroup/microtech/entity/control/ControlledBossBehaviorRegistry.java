package Infinitygroup.microtech.entity.control;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;

import java.util.HashMap;
import java.util.Map;

public final class ControlledBossBehaviorRegistry {
    private static final Map<EntityType<?>, ControlledBossBehavior> CUSTOM = new HashMap<>();

    private ControlledBossBehaviorRegistry() {
    }

    public static void registerBossBehavior(EntityType<?> type, ControlledBossBehavior behavior) {
        CUSTOM.put(type, behavior);
    }

    public static ControlledBossBehavior get(Mob mob) {
        ControlledBossBehavior custom = CUSTOM.get(mob.getType());
        if (custom != null) {
            return custom;
        }
        if (mob instanceof WitherBoss) {
            return ControlledWitherBehavior.INSTANCE;
        }
        if (mob instanceof EnderDragon) {
            return ControlledEnderDragonBehavior.INSTANCE;
        }
        if (mob instanceof Warden) {
            return ControlledWardenBossBehavior.INSTANCE;
        }
        if (mob instanceof ElderGuardian) {
            return ControlledElderGuardianBehavior.INSTANCE;
        }
        return DefaultControlledBossBehavior.INSTANCE;
    }
}
