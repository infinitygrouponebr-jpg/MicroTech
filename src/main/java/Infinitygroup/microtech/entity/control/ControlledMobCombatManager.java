package Infinitygroup.microtech.entity.control;

import Infinitygroup.microtech.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ControlledMobCombatManager {
    private ControlledMobCombatManager() {
    }

    public static ControlledMobCombatRole getRole(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        ControlledMobCombatRole forced = Config.controllerChipForcedRoles.get(id);
        if (forced != null) {
            return forced;
        }

        ControlledMobCombatRole vanilla = detectVanillaRole(mob);
        return Config.controllerChipRoleDenylist.contains(id) ? ControlledMobCombatRole.NONE : vanilla;
    }

    public static boolean canAttack(ControlledMobCombatRole role) {
        return role == ControlledMobCombatRole.MELEE
                || role == ControlledMobCombatRole.RANGED
                || role == ControlledMobCombatRole.MAGIC
                || role == ControlledMobCombatRole.HYBRID;
    }

    public static boolean isSupport(Mob mob) {
        return getRole(mob) == ControlledMobCombatRole.SUPPORT;
    }

    private static ControlledMobCombatRole detectVanillaRole(Mob mob) {
        if (mob instanceof Villager) {
            return ControlledMobCombatRole.NONE;
        }
        if (mob instanceof Warden) {
            return ControlledMobCombatRole.HYBRID;
        }
        if (mob instanceof Creeper) {
            return Config.controllerChipAllowCreeperExplosion ? ControlledMobCombatRole.MELEE : ControlledMobCombatRole.NONE;
        }
        if (mob instanceof SpellcasterIllager || mob instanceof Witch || mob instanceof Blaze) {
            return ControlledMobCombatRole.MAGIC;
        }
        if (mob instanceof CrossbowAttackMob || mob instanceof RangedAttackMob || holdsRangedWeapon(mob)) {
            return ControlledMobCombatRole.RANGED;
        }
        if (isPassiveSupport(mob)) {
            return ControlledMobCombatRole.SUPPORT;
        }
        if (mob instanceof Enemy || mob instanceof net.minecraft.world.entity.animal.AbstractGolem || hasAttackDamage(mob)) {
            return ControlledMobCombatRole.MELEE;
        }
        return ControlledMobCombatRole.NONE;
    }

    private static boolean holdsRangedWeapon(Mob mob) {
        ItemStack main = mob.getMainHandItem();
        ItemStack off = mob.getOffhandItem();
        return main.is(Items.BOW) || main.is(Items.CROSSBOW) || off.is(Items.BOW) || off.is(Items.CROSSBOW);
    }

    private static boolean hasAttackDamage(Mob mob) {
        return mob.getAttribute(Attributes.ATTACK_DAMAGE) != null && mob.getAttributeValue(Attributes.ATTACK_DAMAGE) > 0.0D;
    }

    private static boolean isPassiveSupport(Mob mob) {
        if (mob instanceof Slime slime) {
            return slime.getSize() <= 1;
        }
        return mob instanceof Chicken
                || mob instanceof Pig
                || mob instanceof Cow
                || mob instanceof Sheep
                || mob instanceof Rabbit
                || mob instanceof Bee
                || mob instanceof AbstractHorse
                || mob instanceof Turtle
                || mob instanceof Axolotl
                || mob instanceof net.minecraft.world.entity.ambient.Bat
                || mob instanceof Fox
                || mob instanceof Animal;
    }
}
