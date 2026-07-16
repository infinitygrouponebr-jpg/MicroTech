package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.item.TechCrusherRecipeHelper;
import Infinitygroup.microtech.machine.MachineStatus;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeHost;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import Infinitygroup.microtech.machine.MachineUpgradeType;
import Infinitygroup.microtech.menu.TechCrusherMenu;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TechCrusherBlockEntity extends BlockEntity implements Container, MenuProvider, GeoBlockEntity, MachineUpgradeHost {
    public static final int MAX_ENERGY = 30_000;
    public static final int MAX_RECEIVE = 100;
    public static final int PROCESS_COST = 400;
    public static final int PROCESS_TICKS = 100;
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final int SLOT_COUNT = 2;
    private static final String ENERGY_TAG = "EnergyStored";
    private static final String PROGRESS_TAG = "Progress";
    private static final String ACTIVE_INPUT_TAG = "ActiveInputItem";
    private static final String PROCESSING_ACTIVE_TAG = "ProcessingActive";
    private static final String UPGRADE_TAG = "Upgrades";
    private static final String MACHINE_ID = "microtech:tech_crusher";
    private static final String CONTROLLER_NAME = "crusher_controller";
    private static final String ANIMATION_USE = "use";
    private static final RawAnimation USE_ANIMATION = RawAnimation.begin().thenLoop(ANIMATION_USE);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final CrusherEnergyStorage energyStorage = new CrusherEnergyStorage();
    private final MachineUpgradeInventory upgradeInventory = new MachineUpgradeInventory(MACHINE_ID, 2, () -> {
        this.onUpgradesChanged();
        return true;
    });
    private int progress;
    private String activeInputItemId = "";
    private boolean processingActive;

    public TechCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(Microtech.TECH_CRUSHER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TechCrusherBlockEntity blockEntity) {
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
        int inputInterval = MachineUpgradeHelper.getInputInterval(blockEntity);
        int inputBatch = MachineUpgradeHelper.getInputBatchSize(blockEntity);
        if (inputBatch > 0 && level.getGameTime() % inputInterval == 0L) {
            changed |= blockEntity.tryAutoInput(level, inputBatch);
        }

        ItemStack input = blockEntity.items.get(SLOT_INPUT);

        if (input.isEmpty()) {
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                changed = true;
            }
            blockEntity.setProcessingActive(false);
            if (changed) {
                blockEntity.syncClient();
            }
            return;
        }

        Optional<TechCrusherRecipeHelper.CrusherRecipe> recipe = TechCrusherRecipeHelper.getRecipe(input);
        if (recipe.isEmpty()) {
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                changed = true;
            }
            blockEntity.setProcessingActive(false);
            if (changed) {
                blockEntity.syncClient();
            }
            return;
        }

        ItemStack result = recipe.get().output().copy();
        if (!blockEntity.canAcceptResult(result, input)) {
            blockEntity.setProcessingActive(false);
            if (changed) {
                blockEntity.syncClient();
            }
            return;
        }

        String currentInputId = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        if (!currentInputId.equals(blockEntity.activeInputItemId)) {
            blockEntity.activeInputItemId = currentInputId;
            blockEntity.progress = 0;
            changed = true;
        }

        if (blockEntity.energyStorage.getEnergyStored() < blockEntity.getEffectiveProcessCost()) {
            blockEntity.setProcessingActive(false);
            if (changed) {
                blockEntity.syncClient();
            }
            return;
        }

        int maxProgress = blockEntity.getEffectiveProcessTicks();
        if (blockEntity.progress <= 0) {
            blockEntity.progress = 1;
            blockEntity.setProcessingActive(true);
            changed = true;
        } else {
            blockEntity.progress++;
            blockEntity.setProcessingActive(true);
            changed = true;
        }

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && blockEntity.progress < maxProgress && blockEntity.progress % 10 == 0) {
            blockEntity.playProcessLoopSound(serverLevel, pos);
            blockEntity.spawnProcessParticles(serverLevel, pos, false);
        }

        if (blockEntity.progress >= maxProgress) {
            blockEntity.energyStorage.consumeEnergy(blockEntity.getEffectiveProcessCost());
            blockEntity.finishProcess(result);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                blockEntity.playProcessCompleteSound(serverLevel, pos);
                blockEntity.spawnProcessParticles(serverLevel, pos, true);
            }
            blockEntity.progress = 0;
            blockEntity.setProcessingActive(false);
            changed = true;
        }

        if (changed) {
            blockEntity.syncClient();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.microtech.tech_crusher");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = this.level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(this.level, this.worldPosition);
        return new TechCrusherMenu(containerId, playerInventory, this, access);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        if (tag.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            this.upgradeInventory.deserializeNBT(registries, tag.getCompound(UPGRADE_TAG));
        }
        this.energyStorage.setEnergyStored(tag.getInt(ENERGY_TAG));
        this.progress = Math.max(0, Math.min(PROCESS_TICKS, tag.getInt(PROGRESS_TAG)));
        this.activeInputItemId = tag.getString(ACTIVE_INPUT_TAG);
        this.processingActive = tag.getBoolean(PROCESSING_ACTIVE_TAG);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.put(UPGRADE_TAG, this.upgradeInventory.serializeNBT(registries));
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(PROGRESS_TAG, this.progress);
        tag.putString(ACTIVE_INPUT_TAG, this.activeInputItemId);
        tag.putBoolean(PROCESSING_ACTIVE_TAG, this.processingActive);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(PROGRESS_TAG, this.progress);
        tag.putString(ACTIVE_INPUT_TAG, this.activeInputItemId);
        tag.putBoolean(PROCESSING_ACTIVE_TAG, this.processingActive);
        return tag;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty()) {
            this.setChanged();
            this.syncClient();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(this.items, slot);
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
        this.items.set(slot, copy);
        this.setChanged();
        this.syncClient();
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        this.setChanged();
        this.syncClient();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && TechCrusherRecipeHelper.isValidInput(stack);
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return this.getEffectiveMaxEnergy();
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.getEffectiveProcessTicks();
    }

    public int getEffectiveMaxEnergy() {
        return MAX_ENERGY;
    }

    public int getEffectiveReceiveLimit() {
        return MAX_RECEIVE;
    }

    public int getEffectiveProcessTicks() {
        return Math.max(10, (int) Math.round(PROCESS_TICKS / MachineUpgradeHelper.getSpeedMultiplier(this)));
    }

    public int getEffectiveProcessCost() {
        return Math.max(1, (int) Math.round(PROCESS_COST * MachineUpgradeHelper.getEnergyCostMultiplier(this)));
    }

    public double getYieldChance() {
        return MachineUpgradeHelper.getFortuneChance(this);
    }

    public double getFortuneChance() {
        return MachineUpgradeHelper.getFortuneChance(this);
    }

    public void onUpgradesChanged() {
        this.clampEnergyStored();
        this.syncClient();
    }

    private void clampEnergyStored() {
        int max = this.getEffectiveMaxEnergy();
        if (this.energyStorage.getEnergyStored() > max) {
            this.energyStorage.setEnergyStored(max);
        }
    }

    public boolean isProcessing() {
        return this.processingActive;
    }

    public ItemStack getInputStack() {
        return this.items.get(SLOT_INPUT);
    }

    public ItemStack getOutputStack() {
        return this.items.get(SLOT_OUTPUT);
    }

    public MachineUpgradeInventory getUpgradeInventory() {
        return this.upgradeInventory;
    }

    @Override
    public String getMachineUpgradeId() {
        return MACHINE_ID;
    }

    @Override
    public int getUpgradeSlotCount() {
        return this.upgradeInventory.getSlots();
    }

    public MachineStatus getStatus(Level level) {
        if (this.processingActive && this.progress > 0 && this.canContinueProcessing(level)) {
            return MachineStatus.PROCESSING;
        }

        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return MachineStatus.NO_INPUT;
        }

        Optional<TechCrusherRecipeHelper.CrusherRecipe> recipe = TechCrusherRecipeHelper.getRecipe(input);
        if (recipe.isEmpty()) {
            return MachineStatus.INVALID_ITEM;
        }

        if (!this.canAcceptResult(recipe.get().output(), input)) {
            return MachineStatus.BLOCKED;
        }

        if (this.energyStorage.getEnergyStored() < this.getEffectiveProcessCost()) {
            return MachineStatus.NO_POWER;
        }

        return this.progress > 0 ? MachineStatus.PROCESSING : MachineStatus.READY;
    }

    public boolean canStartProcessing() {
        return this.getStatus(this.level) == MachineStatus.READY;
    }

    public boolean canTakeItemThroughFace(int slot) {
        return slot == SLOT_OUTPUT;
    }

    public boolean canPlaceItemThroughFace(int slot, ItemStack stack) {
        return this.canPlaceItem(slot, stack);
    }

    public void setEnergyStored(int energy) {
        this.energyStorage.setEnergyStored(Math.min(energy, this.getEffectiveMaxEnergy()));
        this.syncClient();
    }

    public ItemStack createItemStackWithEnergy() {
        ItemStack stack = new ItemStack(Microtech.TECH_CRUSHER_ITEM.get());
        saveEnergyToStack(stack, this.energyStorage.getEnergyStored());
        return stack;
    }

    public static void saveEnergyToStack(ItemStack stack, int energy) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(ENERGY_TAG, Math.max(0, Math.min(MAX_ENERGY, energy)));
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
    }

    public static int getEnergyFromStack(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return Math.max(0, Math.min(MAX_ENERGY, tag.getInt(ENERGY_TAG)));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER_NAME, state -> {
            if (!this.shouldAnimateUse()) {
                return PlayState.STOP;
            }

            AnimationController<TechCrusherBlockEntity> controller = state.getController();
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

    private boolean canAcceptResult(ItemStack result, ItemStack input) {
        int totalCopies = this.getPotentialOutputCopies(input);
        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return result.getCount() * totalCopies <= result.getMaxStackSize();
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        int required = output.getCount() + result.getCount() * totalCopies;
        return required <= output.getMaxStackSize();
    }

    private boolean canContinueProcessing(Level level) {
        if (this.progress <= 0) {
            return false;
        }
        if (level == null) {
            return false;
        }

        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }

        Optional<TechCrusherRecipeHelper.CrusherRecipe> recipe = TechCrusherRecipeHelper.getRecipe(input);
        if (recipe.isEmpty()) {
            return false;
        }

        if (!this.canAcceptResult(recipe.get().output(), input)) {
            return false;
        }

        if (this.energyStorage.getEnergyStored() < this.getEffectiveProcessCost()) {
            return false;
        }

        String currentInputId = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        return currentInputId.equals(this.activeInputItemId);
    }

    private boolean shouldAnimateUse() {
        return this.processingActive && this.progress > 0 && this.canContinueProcessing(this.level);
    }

    private void setProcessingActive(boolean active) {
        if (this.processingActive == active) {
            return;
        }
        this.processingActive = active;
        this.syncClient();
    }

    private void finishProcess(ItemStack result) {
        ItemStack input = this.items.get(SLOT_INPUT);
        ItemStack output = this.items.get(SLOT_OUTPUT);

        ItemStack placed = result.copy();
        if (output.isEmpty()) {
            this.items.set(SLOT_OUTPUT, placed);
            output = this.items.get(SLOT_OUTPUT);
        } else {
            output.grow(placed.getCount());
        }

        int extraCopies = this.calculateBonusOutputCopies(input);
        for (int i = 0; i < extraCopies; i++) {
            if (output.getCount() + result.getCount() <= output.getMaxStackSize()) {
                output.grow(result.getCount());
            }
        }

        input.shrink(1);
        if (input.isEmpty()) {
            this.items.set(SLOT_INPUT, ItemStack.EMPTY);
            this.activeInputItemId = "";
        } else {
            this.activeInputItemId = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        }

        this.setChanged();
        this.syncClient();
    }

    private boolean tryAutoInput(Level level) {
        ItemStack current = this.items.get(SLOT_INPUT);
        if (!current.isEmpty() && current.getCount() >= current.getMaxStackSize()) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, this.worldPosition.relative(direction), direction.getOpposite());
            if (handler == null) {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stackInSlot = handler.getStackInSlot(slot);
                if (stackInSlot.isEmpty() || !TechCrusherRecipeHelper.isValidInput(stackInSlot)) {
                    continue;
                }

                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stackInSlot)) {
                    continue;
                }

                ItemStack extracted = handler.extractItem(slot, 1, false);
                if (extracted.isEmpty()) {
                    continue;
                }

                ItemStack inputSlot = this.items.get(SLOT_INPUT);
                if (inputSlot.isEmpty()) {
                    this.items.set(SLOT_INPUT, extracted.copy());
                } else if (ItemStack.isSameItemSameComponents(inputSlot, extracted) && inputSlot.getCount() < inputSlot.getMaxStackSize()) {
                    inputSlot.grow(extracted.getCount());
                } else {
                    ItemHandlerHelper.insertItemStacked(handler, extracted, false);
                    continue;
                }

                this.setChanged();
                this.syncClient();
                return true;
            }
        }

        return false;
    }

    private boolean tryAutoInput(Level level, int attempts) {
        boolean moved = false;
        for (int i = 0; i < attempts; i++) {
            moved |= this.tryAutoInput(level);
        }
        return moved;
    }

    private boolean tryAutoOutput(Level level) {
        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return false;
        }

        boolean moved = false;
        for (Direction direction : Direction.values()) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, this.worldPosition.relative(direction), direction.getOpposite());
            if (handler == null) {
                continue;
            }

            ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, output.copy(), true);
            int transferable = output.getCount() - remaining.getCount();
            if (transferable <= 0) {
                continue;
            }

            ItemStack toMove = output.copy();
            toMove.setCount(transferable);
            ItemStack inserted = ItemHandlerHelper.insertItemStacked(handler, toMove, false);
            int movedCount = transferable - inserted.getCount();
            if (movedCount > 0) {
                output.shrink(movedCount);
                moved = true;
                if (output.isEmpty()) {
                    this.items.set(SLOT_OUTPUT, ItemStack.EMPTY);
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

    private int getPotentialOutputCopies(ItemStack input) {
        return 1 + this.getFortuneMaxBonusCopies(input);
    }

    private int calculateBonusOutputCopies(ItemStack input) {
        int fortuneCount = this.getFortuneEffectiveCount(input);
        int extra = fortuneCount / 8;
        int remainder = fortuneCount % 8;
        if (remainder > 0) {
            net.minecraft.util.RandomSource random = this.level != null ? this.level.getRandom() : net.minecraft.util.RandomSource.create();
            if (random.nextDouble() < (remainder / 8.0D)) {
                extra++;
            }
        }
        return extra;
    }

    private int getFortuneEffectiveCount(ItemStack input) {
        String id = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        int cap = id.contains("ancient_debris") ? 8 : 64;
        return MachineUpgradeHelper.getEffectiveCount((MachineUpgradeHost) this, MachineUpgradeType.FORTUNE, cap);
    }

    private int getFortuneMaxBonusCopies(ItemStack input) {
        int fortuneCount = this.getFortuneEffectiveCount(input);
        return fortuneCount / 8 + (fortuneCount % 8 > 0 ? 1 : 0);
    }

    private void playProcessLoopSound(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        if (MachineUpgradeHelper.getSilenced(this)) {
            return;
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.BLOCKS, 0.35F, 1.15F);
    }

    private void playProcessCompleteSound(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        if (MachineUpgradeHelper.getSilenced(this)) {
            return;
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.15F);
    }

    private void spawnProcessParticles(net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean complete) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 1.08D;
        double centerZ = pos.getZ() + 0.5D;
        Vector3f color = new Vector3f(0.78F, 0.80F, 0.84F);
        level.sendParticles(new DustParticleOptions(color, 1.0F), centerX, centerY, centerZ, complete ? 10 : 4, 0.13D, 0.08D, 0.13D, 0.01D);
        level.sendParticles(ParticleTypes.POOF, centerX, centerY + 0.02D, centerZ, complete ? 6 : 2, 0.10D, 0.06D, 0.10D, 0.01D);
        level.sendParticles(ParticleTypes.SMOKE, centerX, centerY + 0.03D, centerZ, complete ? 2 : 1, 0.08D, 0.04D, 0.08D, 0.0D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, centerX, centerY + 0.12D, centerZ, complete ? 4 : 1, 0.08D, 0.06D, 0.08D, 0.01D);
    }

    private void syncClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private final class CrusherEnergyStorage extends EnergyStorage {
        private CrusherEnergyStorage() {
            super(MAX_ENERGY, MAX_RECEIVE, 0, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int limit = TechCrusherBlockEntity.this.getEffectiveReceiveLimit();
            int space = TechCrusherBlockEntity.this.getEffectiveMaxEnergy() - this.energy;
            int received = Math.max(0, Math.min(toReceive, Math.min(limit, space)));
            if (!simulate && received > 0) {
                this.energy += received;
                TechCrusherBlockEntity.this.syncClient();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return TechCrusherBlockEntity.this.getEffectiveMaxEnergy();
        }

        @Override
        public boolean canReceive() {
            return this.energy < TechCrusherBlockEntity.this.getEffectiveMaxEnergy();
        }

        private void setEnergyStored(int energy) {
            this.energy = Math.max(0, Math.min(TechCrusherBlockEntity.this.getEffectiveMaxEnergy(), energy));
        }

        private void consumeEnergy(int amount) {
            if (amount <= 0) {
                return;
            }

            int consumed = Math.min(this.energy, amount);
            if (consumed > 0) {
                this.energy -= consumed;
                TechCrusherBlockEntity.this.syncClient();
            }
        }
    }
}
