package Infinitygroup.microtech.item;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.energy.EnergyEndpointInfo;
import Infinitygroup.microtech.energy.EnergyNetworkHelper;
import Infinitygroup.microtech.energy.EnergyNetworkInfo;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeHost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyDebugToolItem extends Item {
    private static final int MAX_ENDPOINTS_TO_SHOW = 5;

    public EnergyDebugToolItem(Properties properties) {
        super(properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());

        if (context.getLevel().getBlockState(context.getClickedPos()).is(Microtech.CABLE_T1.get())) {
            EnergyNetworkInfo info = EnergyNetworkHelper.analyzeNetwork(level, context.getClickedPos());
            this.sendCableInfo(player, info);
            return InteractionResult.CONSUME;
        }

        EnergyStorageSnapshot snapshot = this.inspectEnergyStorage(level, context);
        if (snapshot == null) {
            player.displayClientMessage(Component.translatable("message.microtech.energy_debug_tool.no_energy_capability").withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.CONSUME;
        }

        this.sendStorageInfo(player, snapshot, blockEntity);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.microtech.energy_debug_tool").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.microtech.energy_debug_tool.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void sendCableInfo(net.minecraft.world.entity.player.Player player, EnergyNetworkInfo info) {
        player.displayClientMessage(Component.translatable("message.microtech.energy_debug_tool.header").withStyle(ChatFormatting.AQUA), true);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.summary.cables",
                info.cableCount(),
                info.maxCableCount()
        ).withStyle(ChatFormatting.GRAY), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.summary.sources",
                info.sourceCount()
        ).withStyle(ChatFormatting.GRAY), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.summary.targets",
                info.targetCount()
        ).withStyle(ChatFormatting.GRAY), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.summary.available",
                MicroTechTooltipHelper.formatFE(info.totalAvailable())
        ).withStyle(ChatFormatting.GOLD), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.summary.demand",
                MicroTechTooltipHelper.formatFE(info.totalDemand())
        ).withStyle(ChatFormatting.GOLD), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.summary.network_limit",
                MicroTechTooltipHelper.formatFE(info.networkLimit())
        ).withStyle(ChatFormatting.GRAY), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.summary.estimated_transfer",
                MicroTechTooltipHelper.formatFE(info.estimatedTransfer())
        ).withStyle(ChatFormatting.GREEN), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.summary.controller",
                this.formatPos(info.controller())
        ).withStyle(ChatFormatting.AQUA), false);

        if (MicroTechTooltipHelper.isShiftDown()) {
            this.sendDetailedEndpoints(player, info);
        }
    }

    private void sendDetailedEndpoints(net.minecraft.world.entity.player.Player player, EnergyNetworkInfo info) {
        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(Component.translatable("message.microtech.energy_debug_tool.sources_title").withStyle(ChatFormatting.AQUA), false);
        this.sendEndpointList(player, info.sources(), true);
        player.displayClientMessage(Component.translatable("message.microtech.energy_debug_tool.targets_title").withStyle(ChatFormatting.AQUA), false);
        this.sendEndpointList(player, info.targets(), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.over_limit",
                this.localizedBool(info.overLimit())
        ).withStyle(info.overLimit() ? ChatFormatting.RED : ChatFormatting.GRAY), false);
    }

    private void sendEndpointList(net.minecraft.world.entity.player.Player player, List<EnergyEndpointInfo> endpoints, boolean sourceList) {
        int shown = Math.min(MAX_ENDPOINTS_TO_SHOW, endpoints.size());
        for (int i = 0; i < shown; i++) {
            EnergyEndpointInfo endpoint = endpoints.get(i);
            player.displayClientMessage((sourceList
                    ? Component.translatable(
                    "message.microtech.energy_debug_tool.source_entry",
                    i + 1,
                    this.formatPos(endpoint.pos()),
                    MicroTechTooltipHelper.formatFE(endpoint.available())
            )
                    : Component.translatable(
                    "message.microtech.energy_debug_tool.target_entry",
                    i + 1,
                    this.formatPos(endpoint.pos()),
                    MicroTechTooltipHelper.formatFE(endpoint.demand())
            )).withStyle(ChatFormatting.GRAY), false);
        }

        if (endpoints.size() > MAX_ENDPOINTS_TO_SHOW) {
            player.displayClientMessage(Component.translatable(
                    "message.microtech.energy_debug_tool.more",
                    endpoints.size() - MAX_ENDPOINTS_TO_SHOW
            ).withStyle(ChatFormatting.DARK_GRAY), false);
        }
    }

    private void sendStorageInfo(net.minecraft.world.entity.player.Player player, EnergyStorageSnapshot snapshot, BlockEntity blockEntity) {
        player.displayClientMessage(Component.translatable("message.microtech.energy_debug_tool.header").withStyle(ChatFormatting.AQUA), true);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.storage",
                MicroTechTooltipHelper.formatFE(snapshot.stored),
                MicroTechTooltipHelper.formatFE(snapshot.max)
        ).withStyle(ChatFormatting.GOLD), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.can_receive",
                this.localizedBool(snapshot.canReceive)
        ).withStyle(ChatFormatting.GRAY), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.can_extract",
                this.localizedBool(snapshot.canExtract)
        ).withStyle(ChatFormatting.GRAY), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.simulated_input",
                MicroTechTooltipHelper.formatFE(snapshot.simulatedInput)
        ).withStyle(ChatFormatting.AQUA), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.simulated_output",
                MicroTechTooltipHelper.formatFE(snapshot.simulatedOutput)
        ).withStyle(ChatFormatting.AQUA), false);
        player.displayClientMessage(Component.translatable(
                "message.microtech.energy_debug_tool.role",
                Component.translatable(snapshot.roleTranslationKey)
        ).withStyle(ChatFormatting.WHITE), false);

        if (MicroTechTooltipHelper.isShiftDown() && blockEntity instanceof MachineUpgradeHost) {
            List<Component> installedUpgrades = MachineUpgradeHelper.getInstalledUpgrades(blockEntity);
            if (!installedUpgrades.isEmpty()) {
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.translatable("message.microtech.energy_debug_tool.upgrades_title").withStyle(ChatFormatting.AQUA), false);
                for (Component line : installedUpgrades) {
                    player.displayClientMessage(line.copy().withStyle(ChatFormatting.GRAY), false);
                }
                List<Component> effectSummary = MachineUpgradeHelper.getUpgradeEffectSummary(blockEntity);
                if (!effectSummary.isEmpty()) {
                    player.displayClientMessage(Component.literal(""), false);
                    for (Component line : effectSummary) {
                        player.displayClientMessage(line.copy().withStyle(ChatFormatting.DARK_AQUA), false);
                    }
                }
            }
        }
    }

    private EnergyStorageSnapshot inspectEnergyStorage(Level level, UseOnContext context) {
        var clickedPos = context.getClickedPos();
        var clickedFace = context.getClickedFace();
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, clickedPos, clickedFace);
        if (storage == null) {
            for (var direction : net.minecraft.core.Direction.values()) {
                storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, clickedPos, direction);
                if (storage != null) {
                    break;
                }
            }
        }

        if (storage == null) {
            return null;
        }

        int stored = storage.getEnergyStored();
        int max = storage.getMaxEnergyStored();
        int simulatedInput = Math.max(0, storage.receiveEnergy(max > 0 ? Math.min(100, max) : 100, true));
        int simulatedOutput = Math.max(0, storage.extractEnergy(max > 0 ? Math.min(100, max) : 100, true));
        String roleTranslationKey = this.getRoleTranslationKey(storage, max);

        return new EnergyStorageSnapshot(stored, max, storage.canReceive(), storage.canExtract(), simulatedInput, simulatedOutput, roleTranslationKey);
    }

    private String getRoleTranslationKey(IEnergyStorage storage, int max) {
        if (storage.canExtract() && storage.canReceive()) {
            return "message.microtech.energy_debug_tool.role.source_target";
        }
        if (storage.canExtract()) {
            return "message.microtech.energy_debug_tool.role.source";
        }
        if (storage.canReceive()) {
            return "message.microtech.energy_debug_tool.role.target";
        }
        if (max > 0) {
            return "message.microtech.energy_debug_tool.role.storage";
        }
        return "message.microtech.energy_debug_tool.role.none";
    }

    private String formatPos(net.minecraft.core.BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private Component localizedBool(boolean value) {
        return Component.translatable(value
                ? "message.microtech.energy_debug_tool.bool.true"
                : "message.microtech.energy_debug_tool.bool.false");
    }

    private record EnergyStorageSnapshot(int stored, int max, boolean canReceive, boolean canExtract, int simulatedInput, int simulatedOutput, String roleTranslationKey) {
    }
}
