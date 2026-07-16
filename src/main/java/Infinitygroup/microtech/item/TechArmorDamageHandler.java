package Infinitygroup.microtech.item;

import Infinitygroup.microtech.Microtech;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = Microtech.MODID)
public final class TechArmorDamageHandler {
    private TechArmorDamageHandler() {
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        if (!TechArmorEnergyHelper.isFullTechArmorSet(player)) {
            return;
        }

        float incomingDamage = event.getNewDamage();
        if (incomingDamage <= 0) {
            return;
        }

        // TODO: future shield/defense chip may use energy to reduce incoming damage.
        int requiredEnergy = (int) Math.ceil(incomingDamage * TechArmorEnergyHelper.FE_PER_DAMAGE);
        TechArmorEnergyHelper.consumeEnergyFromArmor(player, requiredEnergy);
    }

    @SubscribeEvent
    public static void onArmorHurt(ArmorHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player) || player.level().isClientSide) {
            return;
        }

        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            ItemStack stack = event.getArmorItemStack(slot);
            if (TechArmorEnergyHelper.isTechArmorPiece(stack)) {
                event.setNewDamage(slot, 0.0F);
            }
        }
    }
}
