package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.item.TechMinerTargetHelper;
import Infinitygroup.microtech.machine.MachineStatus;
import Infinitygroup.microtech.machine.MachineUpgradeHost;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import Infinitygroup.microtech.menu.TechMinerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class TechMinerBlockEntity extends BlockEntity implements Container, MenuProvider, GeoBlockEntity, MachineUpgradeHost {
    public static final int MAX_ENERGY = 50_000;
    public static final int MAX_RECEIVE = 100;
    public static final int SCAN_COST = 500;
    public static final int MINE_COST = 250;
    // TODO: Speed Chip deve reduzir este tempo no futuro.
    public static final int PROCESS_TICKS = 160;
    public static final int SCAN_RADIUS = 6;
    public static final int SCAN_DEPTH = 8;
    public static final int OUTPUT_SLOTS = 27;
    public static final int UPGRADE_SLOTS = 4;
    public static final int MAX_FILTER_ENTRIES = 9;
    private static final int MAX_SCANNED_TARGETS = 64;
    private static final String ENERGY_TAG = "EnergyStored";
    private static final String ITEMS_TAG = "Items";
    private static final String TARGETS_TAG = "Targets";
    private static final String SUMMARY_TAG = "Summary";
    private static final String PROCESS_TICKS_TAG = "ProcessTicks";
    private static final String HAS_SCAN_RESULT_TAG = "HasScanResult";
    private static final String PROCESSING_ACTIVE_TAG = "ProcessingActive";
    private static final String MANUALLY_PAUSED_TAG = "ManuallyPaused";
    private static final String UPGRADE_TAG = "Upgrades";
    private static final String FILTER_TAG = "Filter";
    private static final String MACHINE_ID = "microtech:tech_miner";
    private static final String CONTROLLER_NAME = "main_controller";
    private static final String ANIMATION_USE = "use";
    private static final RawAnimation USE_ANIMATION = RawAnimation.begin().thenLoop(ANIMATION_USE);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> outputItems = NonNullList.withSize(OUTPUT_SLOTS, ItemStack.EMPTY);
    private final MachineUpgradeInventory upgradeInventory = new MachineUpgradeInventory(MACHINE_ID, UPGRADE_SLOTS, () -> {
        this.onUpgradesChanged();
        return true;
    });
    private final List<BlockPos> targetPositions = new ArrayList<>();
    private final List<ResourceSummary> resourceSummaries = new ArrayList<>();
    private final List<ResourceLocation> filterEntries = new ArrayList<>(MAX_FILTER_ENTRIES);
    private final MinerEnergyStorage energyStorage = new MinerEnergyStorage();
    private int processTicks;
    private boolean hasScanResult;
    private boolean processingActive;
    private boolean manuallyPaused;

    public TechMinerBlockEntity(BlockPos pos, BlockState state) {
        super(Microtech.TECH_MINER_BLOCK_ENTITY.get(), pos, state);
        for (int i = 0; i < MAX_FILTER_ENTRIES; i++) {
            this.filterEntries.add(null);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TechMinerBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.isRemoved()) {
            return;
        }

        blockEntity.clampEnergyStored();
        boolean changed = false;

        int outputInterval = MachineUpgradeHelper.getOutputInterval(blockEntity);
        int outputBatch = MachineUpgradeHelper.getOutputBatchSize(blockEntity);
        if (outputBatch > 0 && level.getGameTime() % outputInterval == 0L) {
            changed |= blockEntity.tryAutoOutput(level, outputBatch);
        }

        if (!blockEntity.processingActive) {
            blockEntity.processTicks = 0;
            blockEntity.setProcessingActive(false);
        } else if (blockEntity.processTicks > 0) {
            if (!blockEntity.canContinueProcessing(level)) {
                blockEntity.setProcessingActive(false);
                blockEntity.processTicks = 0;
                changed = true;
            } else {
                blockEntity.processTicks--;
                changed = true;

                if (level instanceof ServerLevel serverLevel) {
                    blockEntity.spawnExtractionLaser(serverLevel, pos, blockEntity.targetPositions.getFirst());
                    if (blockEntity.processTicks % 20 == 0) {
                        blockEntity.playExtractionLoopSound(serverLevel, pos);
                    }
                }
            }
        } else if (!blockEntity.targetPositions.isEmpty()) {
            if (blockEntity.tryMineNextTarget(level, pos)) {
                changed = true;
            }

            if (!blockEntity.targetPositions.isEmpty() && blockEntity.canProcessNextBlock(level)) {
                blockEntity.processTicks = blockEntity.getEffectiveProcessTicks();
                blockEntity.setProcessingActive(true);
                changed = true;
            } else {
                blockEntity.setProcessingActive(false);
                blockEntity.processTicks = 0;
                changed = true;
            }
        } else {
            blockEntity.setProcessingActive(false);
            blockEntity.processTicks = 0;
        }

        if (changed) {
            blockEntity.setChanged();
            blockEntity.syncClient();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.microtech.tech_miner");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = this.level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(this.level, this.worldPosition);
        return new TechMinerMenu(containerId, playerInventory, this, access);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.outputItems, registries);
        if (tag.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            this.upgradeInventory.deserializeNBT(registries, tag.getCompound(UPGRADE_TAG));
        }
        this.energyStorage.setEnergyStored(tag.getInt(ENERGY_TAG));
        this.processTicks = Math.max(0, tag.getInt(PROCESS_TICKS_TAG));
        this.hasScanResult = tag.getBoolean(HAS_SCAN_RESULT_TAG);
        this.processingActive = tag.contains(PROCESSING_ACTIVE_TAG, Tag.TAG_BYTE) && tag.getBoolean(PROCESSING_ACTIVE_TAG);
        this.manuallyPaused = tag.contains(MANUALLY_PAUSED_TAG, Tag.TAG_BYTE) && tag.getBoolean(MANUALLY_PAUSED_TAG);

        this.targetPositions.clear();
        if (tag.contains(TARGETS_TAG, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(TARGETS_TAG, Tag.TAG_COMPOUND);
            for (Tag entryTag : listTag) {
                if (!(entryTag instanceof CompoundTag entry)) {
                    continue;
                }
                this.targetPositions.add(BlockPos.of(entry.getLong("Pos")));
                if (this.targetPositions.size() >= this.getEffectiveMaxScannedTargets()) {
                    break;
                }
            }
        }

        this.resourceSummaries.clear();
        if (tag.contains(SUMMARY_TAG, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(SUMMARY_TAG, Tag.TAG_COMPOUND);
            for (Tag entryTag : listTag) {
                if (!(entryTag instanceof CompoundTag entry)) {
                    continue;
                }
                String id = entry.getString("Id");
                int count = entry.getInt("Count");
                if (!id.isBlank() && count > 0) {
                    this.resourceSummaries.add(new ResourceSummary(id, count));
                }
            }
        }

        this.loadFilterEntries(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.outputItems, registries);
        tag.put(UPGRADE_TAG, this.upgradeInventory.serializeNBT(registries));
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(PROCESS_TICKS_TAG, this.processTicks);
        tag.putBoolean(HAS_SCAN_RESULT_TAG, this.hasScanResult);
        tag.putBoolean(PROCESSING_ACTIVE_TAG, this.processingActive);
        tag.putBoolean(MANUALLY_PAUSED_TAG, this.manuallyPaused);

        ListTag targetTag = new ListTag();
        for (BlockPos targetPosition : this.targetPositions) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", targetPosition.asLong());
            targetTag.add(entry);
        }
        tag.put(TARGETS_TAG, targetTag);

        ListTag summaryTag = new ListTag();
        for (ResourceSummary summary : this.resourceSummaries) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", summary.blockId());
            entry.putInt("Count", summary.count());
            summaryTag.add(entry);
        }
        tag.put(SUMMARY_TAG, summaryTag);
        this.saveFilterEntries(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ContainerHelper.saveAllItems(tag, this.outputItems, registries);
        tag.put(UPGRADE_TAG, this.upgradeInventory.serializeNBT(registries));
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(PROCESS_TICKS_TAG, this.processTicks);
        tag.putBoolean(HAS_SCAN_RESULT_TAG, this.hasScanResult);
        tag.putBoolean(PROCESSING_ACTIVE_TAG, this.processingActive);
        tag.putBoolean(MANUALLY_PAUSED_TAG, this.manuallyPaused);

        ListTag targetTag = new ListTag();
        for (BlockPos targetPosition : this.targetPositions) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", targetPosition.asLong());
            targetTag.add(entry);
        }
        tag.put(TARGETS_TAG, targetTag);

        ListTag summaryTag = new ListTag();
        for (ResourceSummary summary : this.resourceSummaries) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", summary.blockId());
            entry.putInt("Count", summary.count());
            summaryTag.add(entry);
        }
        tag.put(SUMMARY_TAG, summaryTag);
        this.saveFilterEntries(tag);
        return tag;
    }

    @Override
    public int getContainerSize() {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.outputItems) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.outputItems.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.outputItems, slot, amount);
        if (!stack.isEmpty()) {
            this.setChanged();
            this.syncClient();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(this.outputItems, slot);
        if (!stack.isEmpty()) {
            this.setChanged();
            this.syncClient();
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.isEmpty() && copy.getCount() > this.getMaxStackSize()) {
            copy.setCount(this.getMaxStackSize());
        }
        this.outputItems.set(slot, copy);
        this.setChanged();
        this.syncClient();
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            this.outputItems.set(i, ItemStack.EMPTY);
        }
        this.setChanged();
        this.syncClient();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER_NAME, state -> {
            if (!this.shouldAnimateUse()) {
                return PlayState.STOP;
            }

            AnimationController<TechMinerBlockEntity> controller = state.getController();
            if (controller.getCurrentRawAnimation() == null || !controller.getCurrentRawAnimation().equals(USE_ANIMATION)) {
                controller.setAnimation(USE_ANIMATION);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public MachineUpgradeInventory getUpgradeInventory() {
        return this.upgradeInventory;
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return this.getEffectiveMaxEnergy();
    }

    public int getProcessTicks() {
        return this.processTicks;
    }

    public int getProcessDuration() {
        return this.getEffectiveProcessTicks();
    }

    @Override
    public String getMachineUpgradeId() {
        return MACHINE_ID;
    }

    @Override
    public int getUpgradeSlotCount() {
        return this.upgradeInventory.getSlots();
    }

    public int getEffectiveMaxEnergy() {
        return Math.max(MAX_ENERGY, (int) Math.round(MAX_ENERGY * MachineUpgradeHelper.getCapacityMultiplier(this)));
    }

    public int getEffectiveReceiveLimit() {
        return Math.max(MAX_RECEIVE, MAX_RECEIVE + MachineUpgradeHelper.getTransferBonus(this));
    }

    public int getEffectiveMineCost() {
        return Math.max(1, (int) Math.round(MINE_COST * MachineUpgradeHelper.getEnergyCostMultiplier(this)));
    }

    public int getEffectiveProcessTicks() {
        return Math.max(20, (int) Math.round(PROCESS_TICKS / MachineUpgradeHelper.getSpeedMultiplier(this)));
    }

    private int getEffectiveScanRadius() {
        return Math.min(38, SCAN_RADIUS + MachineUpgradeHelper.getRangeBonus(this));
    }

    private int getEffectiveScanDepth() {
        return Math.min(72, SCAN_DEPTH + MachineUpgradeHelper.getDepthBonus(this));
    }

    private int getEffectiveMaxScannedTargets() {
        return MachineUpgradeHelper.getAreaTargetLimit(this);
    }

    public boolean isProcessing() {
        return this.processingActive;
    }

    public boolean hasScanResult() {
        return this.hasScanResult;
    }

    public boolean isManuallyPaused() {
        return this.manuallyPaused;
    }

    public void onUpgradesChanged() {
        this.clampEnergyStored();
        this.syncClient();
    }

    public int getFilterTier() {
        return MachineUpgradeHelper.getFilterTier(this);
    }

    public int getFilterCapacity() {
        return MachineUpgradeHelper.getFilterCapacity(this);
    }

    public boolean hasFilterUpgrade() {
        return this.getFilterCapacity() > 0;
    }

    public int getActiveFilterEntryCount() {
        int capacity = this.getFilterCapacity();
        int count = 0;
        for (int i = 0; i < Math.min(capacity, MAX_FILTER_ENTRIES); i++) {
            if (this.filterEntries.get(i) != null) {
                count++;
            }
        }
        return count;
    }

    public ResourceLocation getFilterEntry(int index) {
        if (index < 0 || index >= MAX_FILTER_ENTRIES) {
            return null;
        }
        return this.filterEntries.get(index);
    }

    public ItemStack getFilterDisplayStack(int index) {
        ResourceLocation entry = this.getFilterEntry(index);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.BLOCK.getOptional(entry)
                .filter(block -> block != Blocks.AIR)
                .map(block -> new ItemStack(block.asItem()))
                .filter(stack -> !stack.isEmpty())
                .orElse(ItemStack.EMPTY);
    }

    public boolean setFilterEntry(int index, ResourceLocation blockId, Player player) {
        int capacity = this.getFilterCapacity();
        if (index < 0 || index >= MAX_FILTER_ENTRIES || index >= capacity || blockId == null) {
            return false;
        }

        Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(Blocks.AIR);
        BlockState state = block.defaultBlockState();
        if (block == Blocks.AIR || !TechMinerTargetHelper.isValidTarget(state)) {
            if (player != null && !player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.microtech.tech_miner.filter_invalid"), true);
            }
            return false;
        }

        for (int i = 0; i < MAX_FILTER_ENTRIES; i++) {
            ResourceLocation existing = this.filterEntries.get(i);
            if (i != index && blockId.equals(existing)) {
                if (player != null && !player.level().isClientSide) {
                    player.displayClientMessage(Component.translatable("message.microtech.tech_miner.filter_duplicate"), true);
                }
                return false;
            }
        }

        this.filterEntries.set(index, blockId);
        this.syncClient();
        return true;
    }

    public boolean removeFilterEntry(int index) {
        int capacity = this.getFilterCapacity();
        if (index < 0 || index >= MAX_FILTER_ENTRIES || index >= capacity) {
            return false;
        }
        if (this.filterEntries.get(index) == null) {
            return false;
        }
        this.filterEntries.set(index, null);
        this.syncClient();
        return true;
    }

    public boolean clearFilterEntries() {
        boolean changed = false;
        for (int i = 0; i < MAX_FILTER_ENTRIES; i++) {
            if (this.filterEntries.get(i) != null) {
                this.filterEntries.set(i, null);
                changed = true;
            }
        }
        if (changed) {
            this.syncClient();
        }
        return changed;
    }

    public boolean canStartScan() {
        return !this.processingActive && this.processTicks <= 0 && this.energyStorage.getEnergyStored() >= SCAN_COST;
    }

    public boolean startScan(Player player) {
        if (!this.canStartScan()) {
            if (player != null && this.energyStorage.getEnergyStored() < SCAN_COST && !player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.microtech.tech_miner.no_power"), true);
            }
            if (this.level instanceof ServerLevel serverLevel) {
                this.playFailureEffects(serverLevel);
            }
            return false;
        }

        if (this.level == null) {
            return false;
        }

        this.energyStorage.consumeEnergy(SCAN_COST);
        this.targetPositions.clear();
        this.resourceSummaries.clear();
        this.hasScanResult = true;
        this.manuallyPaused = false;

        List<BlockPos> foundTargets = this.scanTargets(this.level, this.worldPosition);
        this.targetPositions.addAll(foundTargets);
        this.resourceSummaries.addAll(this.buildResourceSummaries(this.level, foundTargets));

        int filterLevel = MachineUpgradeHelper.getFilterLevel(this);
        if (!this.isFilterAllowlistActive() && filterLevel > 0 && !this.resourceSummaries.isEmpty()) {
            this.targetPositions.sort(Comparator
                    .comparingInt((BlockPos target) -> TechMinerTargetHelper.isValidTarget(this.level, target, this.level.getBlockState(target))
                            ? MachineUpgradeHelper.getMinerPriorityRank(this.level.getBlockState(target))
                            : Integer.MAX_VALUE)
                    .thenComparingDouble(target -> target.distSqr(this.worldPosition)));

            if (filterLevel >= 4) {
                String preferredBlockId = this.resourceSummaries.stream()
                        .map(ResourceSummary::blockId)
                        .min(Comparator.comparingInt(this::getResourcePriority).thenComparing(id -> id))
                        .orElse(this.resourceSummaries.getFirst().blockId());
                this.targetPositions.removeIf(target -> !TechMinerTargetHelper.getBlockId(this.level.getBlockState(target)).equals(preferredBlockId));
                this.resourceSummaries.removeIf(summary -> !summary.blockId().equals(preferredBlockId));
            }
        } else {
            this.targetPositions.sort(Comparator.comparingDouble(target -> target.distSqr(this.worldPosition)));
        }

        if (this.targetPositions.isEmpty()) {
            this.processTicks = 0;
            this.setChanged();
            this.syncClient();
            if (this.level instanceof ServerLevel serverLevel) {
                this.playScanFailureEffects(serverLevel, true);
            }
            if (player != null && !player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.microtech.tech_miner.no_targets"), true);
            }
            return true;
        }

        this.processTicks = 0;
        this.setProcessingActive(false);
        this.setChanged();
        this.syncClient();
        if (this.level instanceof ServerLevel serverLevel) {
            this.playScanEffects(serverLevel);
        }
        if (player != null && !player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.microtech.tech_miner.scan_complete"), true);
        }
        return true;
    }

    public boolean canStartMining() {
        return !this.processingActive
                && this.processTicks <= 0
                && !this.targetPositions.isEmpty()
                && this.energyStorage.getEnergyStored() >= this.getEffectiveMineCost()
                && this.hasAnyOutputSpaceForNextTarget()
                && this.isCurrentTargetValid();
    }

    public boolean startMining(Player player) {
        if (!this.canStartMining()) {
            if (player != null && !player.level().isClientSide) {
                if (this.targetPositions.isEmpty()) {
                    player.displayClientMessage(Component.translatable("message.microtech.tech_miner.no_targets"), true);
                } else if (this.energyStorage.getEnergyStored() < this.getEffectiveMineCost()) {
                    player.displayClientMessage(Component.translatable("message.microtech.tech_miner.no_power"), true);
                }
            }
            return false;
        }

        this.processingActive = true;
        this.manuallyPaused = false;
        this.processTicks = this.getEffectiveProcessTicks();
        this.syncClient();
        return true;
    }

    public boolean stopMining() {
        if (!this.processingActive && this.processTicks <= 0) {
            return false;
        }

        this.processingActive = false;
        this.processTicks = 0;
        this.manuallyPaused = true;
        this.syncClient();
        return true;
    }

    public MachineStatus getStatus() {
        if (this.processingActive && this.processTicks > 0) {
            return MachineStatus.PROCESSING;
        }

        if (this.targetPositions.isEmpty()) {
            if (this.hasScanResult) {
                return MachineStatus.NO_TARGETS;
            }
            return this.energyStorage.getEnergyStored() <= 0 ? MachineStatus.NO_POWER : MachineStatus.IDLE;
        }

        if (!this.hasAnyOutputSpaceForNextTarget()) {
            return MachineStatus.FULL;
        }

        if (this.energyStorage.getEnergyStored() < this.getEffectiveMineCost()) {
            return MachineStatus.NO_POWER;
        }

        return this.processingActive ? MachineStatus.SCANNING : (this.manuallyPaused ? MachineStatus.PAUSED : MachineStatus.READY);
    }

    public List<BlockPos> getTargetPositions() {
        return List.copyOf(this.targetPositions);
    }

    public List<ResourceSummary> getResourceSummaries() {
        return List.copyOf(this.resourceSummaries);
    }

    public BlockPos getNextTargetPos() {
        NextTargetInfo nextTarget = this.getNextTargetInfo();
        return nextTarget.hasTarget() ? nextTarget.pos() : null;
    }

    public Component getNextTargetDisplayName() {
        NextTargetInfo nextTarget = this.getNextTargetInfo();
        if (!nextTarget.hasTarget()) {
            return Component.translatable("gui.microtech.tech_miner.no_targets");
        }
        return TechMinerTargetHelper.getDisplayName(nextTarget.state());
    }

    public NextTargetInfo getNextTargetInfo() {
        if (this.level == null || this.targetPositions.isEmpty()) {
            return NextTargetInfo.none();
        }

        BlockPos currentTarget = this.processingActive && this.processTicks > 0 ? this.targetPositions.getFirst() : null;
        int startIndex = currentTarget == null ? 0 : 1;
        for (int index = startIndex; index < this.targetPositions.size(); index++) {
            BlockPos candidate = this.targetPositions.get(index);
            if (currentTarget != null && candidate.equals(currentTarget)) {
                continue;
            }
            if (!this.level.hasChunkAt(candidate)) {
                continue;
            }

            BlockState state = this.level.getBlockState(candidate);
            if (!TechMinerTargetHelper.isValidTarget(this.level, candidate, state) || !this.isAllowedByFilter(state)) {
                continue;
            }

            return new NextTargetInfo(true, candidate, state);
        }

        return NextTargetInfo.none();
    }

    public record NextTargetInfo(boolean hasTarget, BlockPos pos, BlockState state) {
        public static NextTargetInfo none() {
            return new NextTargetInfo(false, BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        }
    }

    public void syncClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void clampEnergyStored() {
        int max = this.getEffectiveMaxEnergy();
        if (this.energyStorage.getEnergyStored() > max) {
            this.energyStorage.setEnergyStored(max);
        }
    }

    public void setEnergyStored(int energy) {
        this.energyStorage.setEnergyStored(energy);
        this.syncClient();
    }

    public ItemStack createItemStackWithEnergy() {
        ItemStack stack = new ItemStack(Microtech.TECH_MINER_ITEM.get());
        saveEnergyToStack(stack, this.energyStorage.getEnergyStored());
        return stack;
    }

    public static void saveEnergyToStack(ItemStack stack, int energy) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(ENERGY_TAG, Math.max(0, Math.min(MAX_ENERGY, energy)));
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public static int getEnergyFromStack(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data = stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return Math.max(0, Math.min(MAX_ENERGY, tag.getInt(ENERGY_TAG)));
    }

    private boolean tryMineNextTarget(Level level, BlockPos machinePos) {
        while (!this.targetPositions.isEmpty()) {
            BlockPos targetPos = this.targetPositions.getFirst();
            BlockState state = level.getBlockState(targetPos);
            if (!TechMinerTargetHelper.isValidTarget(level, targetPos, state) || !this.isAllowedByFilter(state)) {
                this.targetPositions.removeFirst();
                this.syncClient();
                continue;
            }

            if (this.energyStorage.getEnergyStored() < this.getEffectiveMineCost()) {
                return false;
            }

            List<ItemStack> drops = this.collectDrops((ServerLevel) level, targetPos, state);
            if (drops.isEmpty()) {
                level.destroyBlock(targetPos, false);
                this.targetPositions.removeFirst();
                this.syncClient();
                continue;
            }

            if (!this.canStoreDrops(drops)) {
                this.playFailureEffects((ServerLevel) level);
                return false;
            }

            if (!level.destroyBlock(targetPos, false)) {
                this.targetPositions.removeFirst();
                this.syncClient();
                continue;
            }
            this.insertDrops(drops);
            this.energyStorage.consumeEnergy(this.getEffectiveMineCost());
            this.targetPositions.removeFirst();
            this.syncClient();
            this.playMiningEffects((ServerLevel) level, targetPos);
            return true;
        }

        this.hasScanResult = true;
        this.setProcessingActive(false);
        this.syncClient();
        return false;
    }

    private boolean canProcessNextBlock(Level level) {
        if (this.processTicks > 0) {
            return true;
        }

        if (this.targetPositions.isEmpty()) {
            return false;
        }

        if (this.energyStorage.getEnergyStored() < this.getEffectiveMineCost()) {
            return false;
        }

        BlockPos nextTarget = this.targetPositions.getFirst();
        BlockState state = level.getBlockState(nextTarget);
        if (!TechMinerTargetHelper.isValidTarget(level, nextTarget, state) || !this.isAllowedByFilter(state)) {
            return false;
        }

        List<ItemStack> drops = this.collectDrops((ServerLevel) level, nextTarget, state);
        return this.canStoreDrops(drops);
    }

    private boolean shouldAnimateUse() {
        return this.processingActive
                && this.processTicks > 0
                && !this.targetPositions.isEmpty()
                && this.energyStorage.getEnergyStored() >= this.getEffectiveMineCost()
                && this.hasAnyOutputSpaceForNextTarget()
                && this.isCurrentTargetValid();
    }

    private boolean canContinueProcessing(Level level) {
        return this.processingActive
                && this.processTicks > 0
                && !this.targetPositions.isEmpty()
                && this.energyStorage.getEnergyStored() >= this.getEffectiveMineCost()
                && this.hasAnyOutputSpaceForNextTarget()
                && this.isCurrentTargetValid(level);
    }

    private void setProcessingActive(boolean active) {
        if (this.processingActive == active) {
            return;
        }

        this.processingActive = active;
        MicroTechMachineStateHelper.setMachineActive(this, active);
        this.syncClient();
    }

    private List<BlockPos> scanTargets(Level level, BlockPos machinePos) {
        List<BlockPos> targets = new ArrayList<>();
        int minX = machinePos.getX() - this.getEffectiveScanRadius();
        int maxX = machinePos.getX() + this.getEffectiveScanRadius();
        int minZ = machinePos.getZ() - this.getEffectiveScanRadius();
        int maxZ = machinePos.getZ() + this.getEffectiveScanRadius();
        int maxY = machinePos.getY() - 1;
        int minY = machinePos.getY() - this.getEffectiveScanDepth();

        for (BlockPos scanPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (targets.size() >= this.getEffectiveMaxScannedTargets()) {
                break;
            }

            BlockState state = level.getBlockState(scanPos);
            if (TechMinerTargetHelper.isValidTarget(level, scanPos, state) && this.isAllowedByFilter(state)) {
                targets.add(scanPos.immutable());
            }
        }

        targets.sort(Comparator.comparingDouble(target -> target.distSqr(machinePos)));
        return targets;
    }

    private List<ResourceSummary> buildResourceSummaries(Level level, List<BlockPos> targets) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (BlockPos targetPos : targets) {
            BlockState state = level.getBlockState(targetPos);
            if (!TechMinerTargetHelper.isValidTarget(level, targetPos, state) || !this.isAllowedByFilter(state)) {
                continue;
            }

            String id = TechMinerTargetHelper.getBlockId(state);
            counts.merge(id, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new ResourceSummary(entry.getKey(), entry.getValue()))
                .toList();
    }

    private int getResourcePriority(String blockId) {
        BlockState state = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(blockId))
                .map(block -> block.defaultBlockState())
                .orElse(null);
        return state == null ? Integer.MAX_VALUE : MachineUpgradeHelper.getMinerPriorityRank(state);
    }

    private boolean isCurrentTargetAllowedByFilter(Level level) {
        if (level == null || this.targetPositions.isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(this.targetPositions.getFirst());
        return this.isAllowedByFilter(state);
    }

    private boolean isAllowedByFilter(BlockState state) {
        if (!this.isFilterAllowlistActive()) {
            return true;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        int capacity = this.getFilterCapacity();
        for (int i = 0; i < Math.min(capacity, MAX_FILTER_ENTRIES); i++) {
            if (blockId.equals(this.filterEntries.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isFilterAllowlistActive() {
        return this.getFilterCapacity() > 0;
    }

    private void loadFilterEntries(CompoundTag tag) {
        for (int i = 0; i < MAX_FILTER_ENTRIES; i++) {
            this.filterEntries.set(i, null);
        }
        if (!tag.contains(FILTER_TAG, Tag.TAG_LIST)) {
            return;
        }

        ListTag listTag = tag.getList(FILTER_TAG, Tag.TAG_COMPOUND);
        for (Tag entryTag : listTag) {
            if (!(entryTag instanceof CompoundTag entry)) {
                continue;
            }
            int index = entry.getInt("Index");
            if (index < 0 || index >= MAX_FILTER_ENTRIES) {
                continue;
            }
            ResourceLocation id;
            try {
                id = ResourceLocation.parse(entry.getString("Id"));
            } catch (Exception ignored) {
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
            if (block != Blocks.AIR && TechMinerTargetHelper.isValidTarget(block.defaultBlockState())) {
                this.filterEntries.set(index, id);
            }
        }
    }

    private void saveFilterEntries(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (int i = 0; i < MAX_FILTER_ENTRIES; i++) {
            ResourceLocation id = this.filterEntries.get(i);
            if (id == null) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Index", i);
            entry.putString("Id", id.toString());
            listTag.add(entry);
        }
        tag.put(FILTER_TAG, listTag);
    }

    private List<ItemStack> collectDrops(ServerLevel level, BlockPos pos, BlockState state) {
        return Block.getDrops(state, level, pos, this, null, ItemStack.EMPTY);
    }

    private boolean tryAutoOutput(Level level) {
        boolean moved = false;
        for (int slot = 0; slot < this.outputItems.size(); slot++) {
            ItemStack stack = this.outputItems.get(slot);
            if (stack.isEmpty()) {
                continue;
            }

            for (var direction : net.minecraft.core.Direction.values()) {
                var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, this.worldPosition.relative(direction), direction.getOpposite());
                if (handler == null) {
                    continue;
                }

                ItemStack simulated = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(handler, stack.copy(), true);
                int transferable = stack.getCount() - simulated.getCount();
                if (transferable <= 0) {
                    continue;
                }

                ItemStack toMove = stack.copy();
                toMove.setCount(transferable);
                ItemStack remaining = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(handler, toMove, false);
                int inserted = transferable - remaining.getCount();
                if (inserted > 0) {
                    stack.shrink(inserted);
                    moved = true;
                    if (stack.isEmpty()) {
                        this.outputItems.set(slot, ItemStack.EMPTY);
                    }
                    break;
                }
            }
        }

        if (moved) {
            this.setChanged();
            this.syncClient();
        }
        return moved;
    }

    private boolean tryAutoOutput(Level level, int attempts) {
        boolean moved = false;
        for (int i = 0; i < attempts; i++) {
            moved |= this.tryAutoOutput(level);
        }
        return moved;
    }

    private void spawnExtractionLaser(ServerLevel level, BlockPos machinePos, BlockPos targetPos) {
        Vec3 origin = Vec3.atBottomCenterOf(machinePos).add(0.0D, 0.45D, 0.0D);
        Vec3 target = Vec3.atCenterOf(targetPos);
        Vec3 delta = target.subtract(origin);
        int steps = Math.max(12, Math.min(24, (int) Math.ceil(delta.length() * 6.0D)));
        Vec3 step = delta.scale(1.0D / steps);
        Vec3 point = origin;

        for (int i = 0; i <= steps; i++) {
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.12F, 0.84F, 1.0F), 1.0F),
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.01D,
                    0.01D,
                    0.01D,
                    0.0D
            );
            point = point.add(step);
        }

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                target.x,
                target.y,
                target.z,
                3,
                0.10D,
                0.10D,
                0.10D,
                0.01D
        );
        level.sendParticles(
                ParticleTypes.END_ROD,
                target.x,
                target.y,
                target.z,
                1,
                0.06D,
                0.06D,
                0.06D,
                0.01D
        );
    }

    private void playExtractionLoopSound(ServerLevel level, BlockPos machinePos) {
        if (MachineUpgradeHelper.getSilenced(this)) {
            return;
        }
        level.playSound(null, machinePos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.18F, 1.75F);
    }

    private void playScanEffects(ServerLevel level) {
        if (MachineUpgradeHelper.getSilenced(this)) {
            return;
        }
        level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 0.45F, 1.35F);
        level.sendParticles(
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.12F, 0.84F, 1.0F), 1.0F),
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 1.1D,
                this.worldPosition.getZ() + 0.5D,
                10,
                0.18D,
                0.08D,
                0.18D,
                0.0D
        );
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 1.0D,
                this.worldPosition.getZ() + 0.5D,
                4,
                0.16D,
                0.05D,
                0.16D,
                0.0D
        );
    }

    private void playMiningEffects(ServerLevel level, BlockPos targetPos) {
        if (MachineUpgradeHelper.getSilenced(this)) {
            return;
        }
        level.playSound(null, targetPos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.35F, 1.35F);
        level.sendParticles(
                new DustParticleOptions(new Vector3f(0.12F, 0.84F, 1.0F), 1.0F),
                targetPos.getX() + 0.5D,
                targetPos.getY() + 0.5D,
                targetPos.getZ() + 0.5D,
                6,
                0.12D,
                0.12D,
                0.12D,
                0.0D
        );
        level.sendParticles(
                ParticleTypes.END_ROD,
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 1.05D,
                this.worldPosition.getZ() + 0.5D,
                2,
                0.08D,
                0.04D,
                0.08D,
                0.0D
        );
    }

    private void playFailureEffects(ServerLevel level) {
        if (MachineUpgradeHelper.getSilenced(this)) {
            return;
        }
        this.playScanFailureEffects(level, false);
    }

    private void playScanFailureEffects(ServerLevel level, boolean noTargets) {
        if (MachineUpgradeHelper.getSilenced(this)) {
            return;
        }
        level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.30F, noTargets ? 0.85F : 0.65F);
        level.sendParticles(
                new DustParticleOptions(new Vector3f(1.0F, 0.25F, 0.25F), 1.0F),
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 1.0D,
                this.worldPosition.getZ() + 0.5D,
                4,
                0.10D,
                0.08D,
                0.10D,
                0.0D
        );
    }

    private boolean canStoreDrops(List<ItemStack> drops) {
        NonNullList<ItemStack> simulated = NonNullList.withSize(OUTPUT_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            simulated.set(i, this.outputItems.get(i).copy());
        }

        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            remaining = insertIntoInventory(simulated, remaining, true);
            if (!remaining.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private void insertDrops(List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            remaining = insertIntoInventory(this.outputItems, remaining, false);
            if (!remaining.isEmpty()) {
                break;
            }
        }
    }

    private boolean hasAnyOutputSpaceForNextTarget() {
        for (ItemStack stack : this.outputItems) {
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentTargetValid() {
        return this.isCurrentTargetValid(this.level);
    }

    private boolean isCurrentTargetValid(Level level) {
        if (level == null || this.targetPositions.isEmpty()) {
            return false;
        }

        BlockPos targetPos = this.targetPositions.getFirst();
        BlockState state = level.getBlockState(targetPos);
        return TechMinerTargetHelper.isValidTarget(level, targetPos, state) && this.isAllowedByFilter(state);
    }

    private static ItemStack insertIntoInventory(List<ItemStack> inventory, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack existing = inventory.get(i);
            if (existing.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                continue;
            }

            int maxStackSize = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize());
            int space = maxStackSize - existing.getCount();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, stack.getCount());
            if (!simulate) {
                existing.grow(moved);
                inventory.set(i, existing);
            }
            stack.shrink(moved);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack existing = inventory.get(i);
            if (!existing.isEmpty()) {
                continue;
            }

            int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
            ItemStack copy = stack.copy();
            copy.setCount(moved);
            if (!simulate) {
                inventory.set(i, copy);
            }
            stack.shrink(moved);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    public record ResourceSummary(String blockId, int count) {
        public Component displayName() {
            BlockState state = BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.ResourceLocation.parse(this.blockId))
                    .map(block -> block.defaultBlockState())
                    .orElse(null);
            if (state == null) {
                return Component.literal(this.blockId);
            }
            return TechMinerTargetHelper.getDisplayName(state);
        }
    }

    private final class MinerEnergyStorage extends EnergyStorage {
        private MinerEnergyStorage() {
            super(MAX_ENERGY, MAX_RECEIVE, 0, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) {
                return 0;
            }

            int limit = Math.max(0, TechMinerBlockEntity.this.getEffectiveReceiveLimit());
            int space = Math.max(0, TechMinerBlockEntity.this.getEffectiveMaxEnergy() - this.energy);
            if (limit <= 0 || space <= 0) {
                return 0;
            }

            int received = Math.max(0, Math.min(toReceive, Math.min(limit, space)));
            if (!simulate && received > 0) {
                this.energy += received;
                TechMinerBlockEntity.this.syncClient();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public boolean canReceive() {
            return TechMinerBlockEntity.this.getEffectiveReceiveLimit() > 0
                    && this.getEnergyStored() < TechMinerBlockEntity.this.getEffectiveMaxEnergy();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        private void setEnergyStored(int energy) {
            this.energy = Math.max(0, Math.min(TechMinerBlockEntity.this.getEffectiveMaxEnergy(), energy));
        }

        private void consumeEnergy(int amount) {
            if (amount <= 0) {
                return;
            }

            this.energy = Math.max(0, this.energy - amount);
            TechMinerBlockEntity.this.syncClient();
        }
    }
}
