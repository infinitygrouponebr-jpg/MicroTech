package Infinitygroup.microtech.client;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.client.screen.TechSwordAbilityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Microtech.MODID, value = Dist.CLIENT)
public final class TechSwordClientEvents {
    private TechSwordClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (TechSwordClientKeybinds.OPEN_ABILITY_SELECTOR.consumeClick()) {
            LocalPlayer player = minecraft.player;
            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty() || stack.getItem() != Microtech.TECH_SWORD.get()) {
                player.displayClientMessage(Component.translatable("message.microtech.tech_sword.ability_requires_sword"), true);
                continue;
            }
            minecraft.setScreen(new TechSwordAbilityScreen(stack.copy()));
        }
    }
}
