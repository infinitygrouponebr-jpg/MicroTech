package Infinitygroup.microtech.item;

import Infinitygroup.microtech.entity.control.ControlledMobManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AdvancedControllerChipItem extends Item {
    public AdvancedControllerChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            return ControlledMobManager.removeChip(mob, serverPlayer) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        ControlledMobManager.ApplyResult result = ControlledMobManager.tryInstallAdvanced(mob, serverPlayer, stack, hand);
        if (!result.success()) {
            player.displayClientMessage(Component.translatable(result.messageKey(), result.args()), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.microtech.advanced_controller_chip.class").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.microtech.advanced_controller_chip.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.advanced_controller_chip.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.advanced_controller_chip.desc3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
