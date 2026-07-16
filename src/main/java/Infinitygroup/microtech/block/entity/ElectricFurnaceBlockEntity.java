package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.menu.ElectricFurnaceMenu;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeHost;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Optional;

public class ElectricFurnaceBlockEntity extends BlockEntity implements Container, MenuProvider, MachineUpgradeHost {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final int SLOT_COUNT = 2;
    public static final int MAX_ENERGY = 10_000;
    public static final int MAX_RECEIVE = 40;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_PROGRESS = 200;
    private static final String ENERGY_TAG = "EnergyStored";
    private static final String PROGRESS_TAG = "Progress";
    private static final String ACTIVE_INPUT_TAG = "ActiveInputItem";
    private static final String UPGRADE_TAG = "Upgrades";
    private static final String MACHINE_ID = "microtech:electric_furnace_t1";

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ElectricFurnaceEnergyStorage energyStorage = new ElectricFurnaceEnergyStorage();
    private final MachineUpgradeInventory upgradeInventory = new MachineUpgradeInventory(MACHINE_ID, 2, () -> {
        this.onUpgradesChanged();
        return true;
    });
    private int progress;
    private String activeInputItemId = "";

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Microtech.ELECTRIC_FURNACE_T1_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity blockEntity) {
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
            return;
        }

        Optional<RecipeHolder<SmeltingRecipe>> recipe = blockEntity.getSmeltingRecipe(level, input);
        if (recipe.isEmpty()) {
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                changed = true;
            }
            String currentInputId = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
            if (!currentInputId.equals(blockEntity.activeInputItemId)) {
                blockEntity.activeInputItemId = currentInputId;
                changed = true;
            }
            if (changed) {
                blockEntity.setChanged();
            }
            return;
        }

        ItemStack result = recipe.get().value().getResultItem(level.registryAccess()).copy();
        if (result.isEmpty() || !blockEntity.canAcceptResult(result, input)) {
            if (changed) {
                blockEntity.setChanged();
            }
            return;
        }

        String currentInputId = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        if (!currentInputId.equals(blockEntity.activeInputItemId)) {
            blockEntity.activeInputItemId = currentInputId;
            blockEntity.progress = 0;
            changed = true;
        }

        if (blockEntity.energyStorage.getEnergyStored() < blockEntity.getEffectiveEnergyPerTick()) {
            if (changed) {
                blockEntity.setChanged();
            }
            return;
        }

        blockEntity.energyStorage.consumeEnergy(blockEntity.getEffectiveEnergyPerTick());
        blockEntity.progress++;
        changed = true;
        blockEntity.syncClient();
        blockEntity.spawnProcessingEffects(level, pos);

        if (blockEntity.progress >= blockEntity.getEffectiveMaxProgress()) {
            blockEntity.finishSmelting(result, input);
        }

        if (changed) {
            blockEntity.setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.microtech.electric_furnace_t1");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = this.level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(this.level, this.worldPosition);
        return new ElectricFurnaceMenu(containerId, playerInventory, this, access);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            this.upgradeInventory.deserializeNBT(registries, tag.getCompound(UPGRADE_TAG));
        }
        this.energyStorage.setEnergyStored(tag.getInt(ENERGY_TAG));
        this.progress = tag.getInt(PROGRESS_TAG);
        this.activeInputItemId = tag.getString(ACTIVE_INPUT_TAG);
        if (this.energyStorage.getEnergyStored() < 0) {
            this.energyStorage.setEnergyStored(0);
        }
        if (this.progress < 0) {
            this.progress = 0;
        } else if (this.progress > MAX_PROGRESS) {
            this.progress = MAX_PROGRESS;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put(UPGRADE_TAG, this.upgradeInventory.serializeNBT(registries));
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(PROGRESS_TAG, this.progress);
        tag.putString(ACTIVE_INPUT_TAG, this.activeInputItemId);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
            syncClient();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(items, slot);
        if (!stack.isEmpty()) {
            syncClient();
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.isEmpty() && copy.getCount() > getMaxStackSize()) {
            copy.setCount(getMaxStackSize());
        }
        items.set(slot, copy);
        setChanged();
        syncClient();
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
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
        syncClient();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && this.canSmelt(stack);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
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

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.getEffectiveMaxProgress();
    }

    @Override
    public String getMachineUpgradeId() {
        return MACHINE_ID;
    }

    @Override
    public int getUpgradeSlotCount() {
        return this.upgradeInventory.getSlots();
    }

    public ItemStack getInputStack() {
        return this.items.get(SLOT_INPUT);
    }

    public ItemStack getOutputStack() {
        return this.items.get(SLOT_OUTPUT);
    }

    public boolean isProcessing() {
        return this.progress > 0 && this.canContinueProcessing(this.level);
    }

    public int getEffectiveMaxEnergy() {
        return MAX_ENERGY;
    }

    public int getEffectiveReceiveLimit() {
        return MAX_RECEIVE;
    }

    public int getEffectiveEnergyPerTick() {
        return Math.max(1, (int) Math.round(ENERGY_PER_TICK * MachineUpgradeHelper.getEnergyCostMultiplier(this)));
    }

    public int getEffectiveMaxProgress() {
        return Math.max(10, (int) Math.round(MAX_PROGRESS / MachineUpgradeHelper.getSpeedMultiplier(this)));
    }

    public double getDoubleSmeltChance() {
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

    public boolean canSmelt(ItemStack stack) {
        return this.level != null && this.getSmeltingRecipe(this.level, stack).isPresent();
    }

    private Optional<RecipeHolder<SmeltingRecipe>> getSmeltingRecipe(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
    }

    private boolean canAcceptResult(ItemStack result, ItemStack inputSnapshot) {
        int totalCopies = this.getPotentialOutputCopies(inputSnapshot);
        ItemStack output = items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return result.getCount() * totalCopies <= result.getMaxStackSize();
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() * totalCopies <= output.getMaxStackSize();
    }

    private void finishSmelting(ItemStack result, ItemStack inputSnapshot) {
        ItemStack input = items.get(SLOT_INPUT);
        ItemStack output = items.get(SLOT_OUTPUT);

        if (output.isEmpty()) {
            items.set(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        int bonusCopies = this.calculateBonusSmeltCopies(inputSnapshot);
        for (int i = 0; i < bonusCopies; i++) {
            if (output.getCount() + result.getCount() <= output.getMaxStackSize()) {
                output.grow(result.getCount());
            }
        }

        input.shrink(1);
        if (input.isEmpty()) {
            items.set(SLOT_INPUT, ItemStack.EMPTY);
        }

        this.progress = 0;
        this.activeInputItemId = input.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        setChanged();
        this.syncClient();
    }

    private int getPotentialOutputCopies(ItemStack inputSnapshot) {
        return 1 + this.getFortuneBonusCopies(inputSnapshot);
    }

    private int calculateBonusSmeltCopies(ItemStack inputSnapshot) {
        return this.getFortuneBonusCopies(inputSnapshot);
    }

    private int getFortuneBonusCopies(ItemStack inputSnapshot) {
        int fortuneCount = MachineUpgradeHelper.getEffectiveCount((MachineUpgradeHost) this, Infinitygroup.microtech.machine.MachineUpgradeType.FORTUNE, 64);
        if (fortuneCount >= 64) {
            return 4;
        }
        if (fortuneCount >= 32) {
            return 3;
        }
        if (fortuneCount >= 16) {
            return 2;
        }
        if (fortuneCount >= 8) {
            return 1;
        }
        return fortuneCount > 0 ? 1 : 0;
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
                if (stackInSlot.isEmpty() || !this.canSmelt(stackInSlot)) {
                    continue;
                }

                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stackInSlot)) {
                    continue;
                }

                ItemStack extracted = handler.extractItem(slot, 1, false);
                if (extracted.isEmpty()) {
                    continue;
                }

                ItemStack input = this.items.get(SLOT_INPUT);
                if (input.isEmpty()) {
                    this.items.set(SLOT_INPUT, extracted.copy());
                } else if (ItemStack.isSameItemSameComponents(input, extracted) && input.getCount() < input.getMaxStackSize()) {
                    input.grow(extracted.getCount());
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

    private void spawnProcessingEffects(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !this.isProcessing()) {
            return;
        }

        if (this.progress % 5 != 0) {
            return;
        }

        double centerX = pos.getX() + 0.5D;
        double topY = pos.getY() + 1.08D;
        double centerZ = pos.getZ() + 0.5D;

        for (int i = 0; i < 2; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5D) * 0.18D;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5D) * 0.18D;
            double offsetY = serverLevel.random.nextDouble() * 0.06D;
            serverLevel.sendParticles(ParticleTypes.FLAME, centerX + offsetX, topY + offsetY, centerZ + offsetZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }

        for (int i = 0; i < 1; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5D) * 0.12D;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5D) * 0.12D;
            serverLevel.sendParticles(ParticleTypes.SMOKE, centerX + offsetX, topY, centerZ + offsetZ, 1, 0.01D, 0.02D, 0.01D, 0.0D);
        }
    }

    private void syncClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    private boolean canContinueProcessing(Level level) {
        if (level == null || this.progress <= 0) {
            return false;
        }

        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }

        Optional<RecipeHolder<SmeltingRecipe>> recipe = this.getSmeltingRecipe(level, input);
        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack result = recipe.get().value().getResultItem(level.registryAccess()).copy();
        if (result.isEmpty() || !this.canAcceptResult(result, input)) {
            return false;
        }

        return this.energyStorage.getEnergyStored() >= this.getEffectiveEnergyPerTick();
    }

    private final class ElectricFurnaceEnergyStorage extends EnergyStorage {
        private ElectricFurnaceEnergyStorage() {
            super(MAX_ENERGY, MAX_RECEIVE, 0, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int limit = ElectricFurnaceBlockEntity.this.getEffectiveReceiveLimit();
            int space = ElectricFurnaceBlockEntity.this.getEffectiveMaxEnergy() - this.energy;
            int received = Math.max(0, Math.min(toReceive, Math.min(limit, space)));
            if (!simulate && received > 0) {
                this.energy += received;
                ElectricFurnaceBlockEntity.this.syncClient();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return ElectricFurnaceBlockEntity.this.getEffectiveMaxEnergy();
        }

        @Override
        public boolean canReceive() {
            return this.energy < ElectricFurnaceBlockEntity.this.getEffectiveMaxEnergy();
        }

        private int consumeEnergy(int amount) {
            if (amount <= 0) {
                return 0;
            }

            int consumed = Math.min(this.energy, amount);
            if (consumed > 0) {
                this.energy -= consumed;
                ElectricFurnaceBlockEntity.this.setChanged();
            }
            return consumed;
        }

        private void setEnergyStored(int energy) {
            this.energy = Math.max(0, Math.min(ElectricFurnaceBlockEntity.this.getEffectiveMaxEnergy(), energy));
        }
    }
}
