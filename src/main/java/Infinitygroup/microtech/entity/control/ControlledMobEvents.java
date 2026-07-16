package Infinitygroup.microtech.entity.control;

import Infinitygroup.microtech.Config;
import Infinitygroup.microtech.Microtech;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class ControlledMobEvents {
    private ControlledMobEvents() {
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (ControlledMobData.isControlled(mob)) {
            ControlledMobManager.installGoals(mob);
        }
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        ControlledMobManager.onMobUnloaded(mob);
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (Config.friendlyFire || event.getEntity().level().isClientSide) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity attacker && ControlledMobManager.areAllies(attacker, event.getEntity())) {
            event.setNewDamage(0.0F);
            if (attacker instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        ControlledMobManager.rememberDamage(event.getEntity(), attacker, event.getEntity().level().getGameTime());
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob) || !ControlledMobData.isControlled(mob)) {
            return;
        }
        if (Config.controllerChipDropsOnControlledMobDeath && mob.level() instanceof ServerLevel serverLevel) {
            mob.spawnAtLocation(new ItemStack(Microtech.CONTROLLER_CHIP.get()));
        }
        ControlledMobManager.onControlledMobRemoved(mob);
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.getHand() != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(event.getTarget() instanceof Mob mob) || !ControlledMobData.isControlledBy(mob, player.getUUID())) {
            return;
        }
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return;
        }

        ControlledMobOrder order;
        if (player.isShiftKeyDown()) {
            ControlledMobData.setGuard(mob, mob.blockPosition(), mob.level().dimension());
            order = ControlledMobOrder.GUARD;
        } else {
            ControlledMobOrder current = ControlledMobData.getOrder(mob);
            order = current == ControlledMobOrder.FOLLOW ? ControlledMobOrder.STAY : ControlledMobOrder.FOLLOW;
            if (order == ControlledMobOrder.STAY) {
                ControlledMobData.setStay(mob, mob.blockPosition(), mob.level().dimension());
            } else {
                ControlledMobData.setOrder(mob, order);
            }
        }
        player.displayClientMessage(ComponentHelper.order(order), true);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
