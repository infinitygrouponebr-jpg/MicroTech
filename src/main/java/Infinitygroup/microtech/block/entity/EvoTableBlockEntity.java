package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.menu.EvoTableMenu;
import Infinitygroup.microtech.item.TechArmorUpgradeHelper;
import Infinitygroup.microtech.item.TechChipItem;
import Infinitygroup.microtech.item.TechChipType;
import Infinitygroup.microtech.item.TechSwordData;
import Infinitygroup.microtech.item.TechSwordItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.core.NonNullList;

public class EvoTableBlockEntity extends BlockEntity implements Container, MenuProvider, GeoBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_COUNT = 2;
    public static final int SWORD_EVOLUTION_DURATION = 50;
    public static final int FLIGHT_CHIP_INSTALL_TIME = 600;
    private static final String ITEMS_TAG = "Items";
    private static final String EVOLVING_TAG = "Evolving";
    private static final String EVOLUTION_TICKS_TAG = "EvolutionTicks";
    private static final String EVOLUTION_DURATION_TAG = "EvolutionDuration";
    private static final String EVOLUTION_MODE_TAG = "EvolutionMode";
    private static final String FEEDBACK_STATE_TAG = "FeedbackState";
    private static final String FEEDBACK_TICKS_TAG = "FeedbackTicks";
    private static final String CONTROLLER_NAME = "main_controller";
    private static final String ANIMATION_USE = "use";
    private static final RawAnimation USE_ANIMATION = RawAnimation.begin().thenPlay(ANIMATION_USE);

    public enum EvoStatus {
        INACTIVE(0, "screen.microtech.evo_table.inactive"),
        INVALID_ITEM(1, "screen.microtech.evo_table.invalid_item"),
        INVALID_CHIP(2, "screen.microtech.evo_table.invalid_chip"),
        READY(3, "screen.microtech.evo_table.ready"),
        EVOLVING(4, "screen.microtech.evo_table.evolving"),
        CHIP_INSTALLED(5, "screen.microtech.evo_table.chip_installed"),
        CHIP_UPGRADED(6, "screen.microtech.evo_table.chip_upgraded"),
        CHIP_MAX_LEVEL(7, "screen.microtech.evo_table.chip_max_level"),
        FLIGHT_INVALID_ITEM(8, "screen.microtech.evo_table.flight_invalid_item"),
        FLIGHT_INVALID_CHIP(9, "screen.microtech.evo_table.flight_invalid_chip"),
        FLIGHT_READY(10, "screen.microtech.evo_table.flight_ready"),
        FLIGHT_INSTALLING(11, "screen.microtech.evo_table.flight_installing"),
        FLIGHT_INSTALLED(12, "screen.microtech.evo_table.flight_installed"),
        FLIGHT_ALREADY_INSTALLED(13, "screen.microtech.evo_table.flight_already_installed");

        private final int id;
        private final String translationKey;

        EvoStatus(int id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public int getId() {
            return this.id;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public static EvoStatus fromId(int id) {
            for (EvoStatus status : values()) {
                if (status.id == id) {
                    return status;
                }
            }
            return INACTIVE;
        }
    }

    private enum EvolutionMode {
        NONE,
        SWORD_CHIP,
        FLIGHT_CHIP
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean evolving;
    private int evolutionTicks;
    private int evolutionDuration = SWORD_EVOLUTION_DURATION;
    private EvolutionMode evolutionMode = EvolutionMode.NONE;
    private boolean useAnimationPlayed;
    private int feedbackState;
    private int feedbackTicks;

    public EvoTableBlockEntity(BlockPos pos, BlockState state) {
        super(Microtech.EVO_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EvoTableBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.isRemoved()) {
            return;
        }

        boolean changed = false;

        if (blockEntity.feedbackTicks > 0) {
            blockEntity.feedbackTicks--;
            changed = true;
        }

        if (blockEntity.evolving) {
            if (!blockEntity.useAnimationPlayed) {
                blockEntity.triggerAnim(CONTROLLER_NAME, ANIMATION_USE);
                blockEntity.useAnimationPlayed = true;
            }

            if (blockEntity.evolutionTicks > 0) {
                blockEntity.evolutionTicks--;
            }

            if (blockEntity.evolutionTicks <= 0) {
                blockEntity.completeEvolution();
                changed = true;
            }
        }

        if (changed) {
            blockEntity.syncClient();
        }

        MicroTechMachineStateHelper.setMachineActive(blockEntity, blockEntity.evolving);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.microtech.evo_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = this.level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(this.level, this.worldPosition);
        return new EvoTableMenu(containerId, playerInventory, this, access);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.evolving = tag.getBoolean(EVOLVING_TAG);
        this.evolutionTicks = tag.contains(EVOLUTION_TICKS_TAG, Tag.TAG_INT) ? tag.getInt(EVOLUTION_TICKS_TAG) : 0;
        if (this.evolutionTicks < 0) {
            this.evolutionTicks = 0;
        }
        this.evolutionDuration = tag.contains(EVOLUTION_DURATION_TAG, Tag.TAG_INT) ? tag.getInt(EVOLUTION_DURATION_TAG) : SWORD_EVOLUTION_DURATION;
        if (this.evolutionDuration <= 0) {
            this.evolutionDuration = SWORD_EVOLUTION_DURATION;
        }
        if (this.evolutionTicks > this.evolutionDuration) {
            this.evolutionTicks = this.evolutionDuration;
        }
        this.useAnimationPlayed = false;
        this.evolutionMode = EvolutionMode.NONE;
        if (tag.contains(EVOLUTION_MODE_TAG, Tag.TAG_STRING)) {
            try {
                this.evolutionMode = EvolutionMode.valueOf(tag.getString(EVOLUTION_MODE_TAG));
            } catch (IllegalArgumentException ignored) {
                this.evolutionMode = EvolutionMode.NONE;
            }
        }
        this.feedbackState = tag.contains(FEEDBACK_STATE_TAG, Tag.TAG_INT) ? tag.getInt(FEEDBACK_STATE_TAG) : EvoStatus.INACTIVE.getId();
        this.feedbackTicks = tag.contains(FEEDBACK_TICKS_TAG, Tag.TAG_INT) ? tag.getInt(FEEDBACK_TICKS_TAG) : 0;
        if (this.feedbackTicks < 0) {
            this.feedbackTicks = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putBoolean(EVOLVING_TAG, this.evolving);
        tag.putInt(EVOLUTION_TICKS_TAG, this.evolutionTicks);
        tag.putInt(EVOLUTION_DURATION_TAG, this.evolutionDuration);
        tag.putString(EVOLUTION_MODE_TAG, this.evolutionMode.name());
        tag.putInt(FEEDBACK_STATE_TAG, this.feedbackState);
        tag.putInt(FEEDBACK_TICKS_TAG, this.feedbackTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putBoolean(EVOLVING_TAG, this.evolving);
        tag.putInt(EVOLUTION_TICKS_TAG, this.evolutionTicks);
        tag.putInt(EVOLUTION_DURATION_TAG, this.evolutionDuration);
        tag.putString(EVOLUTION_MODE_TAG, this.evolutionMode.name());
        tag.putInt(FEEDBACK_STATE_TAG, this.feedbackState);
        tag.putInt(FEEDBACK_TICKS_TAG, this.feedbackTicks);
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
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.isEmpty() && copy.getCount() > this.getMaxStackSize()) {
            copy.setCount(this.getMaxStackSize());
        }
        this.items.set(slot, copy);
        this.feedbackState = EvoStatus.INACTIVE.getId();
        this.feedbackTicks = 0;
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
        if (stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case SLOT_INPUT -> stack.is(Microtech.TECH_SWORD.get()) || TechArmorUpgradeHelper.isTechArmorChestplate(stack);
            case SLOT_MATERIAL -> isValidChip(stack);
            default -> false;
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER_NAME, state -> PlayState.STOP)
                .triggerableAnim(ANIMATION_USE, USE_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public boolean isEvolving() {
        return this.evolving;
    }

    public int getEvolutionTicks() {
        return this.evolutionTicks;
    }

    public int getEvolutionDuration() {
        return this.evolutionDuration;
    }

    public int getFeedbackState() {
        return this.feedbackState;
    }

    public int getFeedbackTicks() {
        return this.feedbackTicks;
    }

    public EvoStatus getStatus() {
        if (this.feedbackTicks > 0) {
            return EvoStatus.fromId(this.feedbackState);
        }
        if (this.evolving) {
            return this.evolutionMode == EvolutionMode.FLIGHT_CHIP ? EvoStatus.FLIGHT_INSTALLING : EvoStatus.EVOLVING;
        }
        if (this.getInputStack().isEmpty() && this.getMaterialStack().isEmpty()) {
            return EvoStatus.INACTIVE;
        }

        if (isValidSword(this.getInputStack())) {
            TechChipType chipType = getSwordChipType(this.getMaterialStack());
            if (chipType == null) {
                return EvoStatus.INVALID_CHIP;
            }
            if (TechSwordData.getChipLevel(this.getInputStack(), chipType) >= chipType.getMaxLevel()) {
                return EvoStatus.CHIP_MAX_LEVEL;
            }
            return EvoStatus.READY;
        }

        if (TechArmorUpgradeHelper.isTechArmorChestplate(this.getInputStack())) {
            if (TechArmorUpgradeHelper.hasFlightChip(this.getInputStack())) {
                return EvoStatus.FLIGHT_ALREADY_INSTALLED;
            }
            if (!isValidFlightChip(this.getMaterialStack())) {
                return this.getMaterialStack().isEmpty() ? EvoStatus.FLIGHT_INVALID_CHIP : EvoStatus.FLIGHT_INVALID_CHIP;
            }
            return EvoStatus.FLIGHT_READY;
        }

        return EvoStatus.INVALID_ITEM;
    }

    public ItemStack getInputStack() {
        return this.getItem(SLOT_INPUT);
    }

    public ItemStack getMaterialStack() {
        return this.getItem(SLOT_MATERIAL);
    }

    public void syncClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    public boolean canStartEvolution() {
        if (this.evolving) {
            return false;
        }

        if (isValidSword(this.getInputStack())) {
            TechChipType chipType = getSwordChipType(this.getMaterialStack());
            return chipType != null && TechSwordData.getChipLevel(this.getInputStack(), chipType) < chipType.getMaxLevel();
        }

        return TechArmorUpgradeHelper.isTechArmorChestplate(this.getInputStack())
                && !TechArmorUpgradeHelper.hasFlightChip(this.getInputStack())
                && isValidFlightChip(this.getMaterialStack());
    }

    public boolean startEvolution() {
        if (!this.canStartEvolution()) {
            return false;
        }

        this.evolving = true;
        this.evolutionMode = isValidSword(this.getInputStack()) ? EvolutionMode.SWORD_CHIP : EvolutionMode.FLIGHT_CHIP;
        this.evolutionDuration = this.evolutionMode == EvolutionMode.FLIGHT_CHIP ? FLIGHT_CHIP_INSTALL_TIME : SWORD_EVOLUTION_DURATION;
        this.evolutionTicks = this.evolutionDuration;
        this.useAnimationPlayed = false;
        this.feedbackState = this.evolutionMode == EvolutionMode.FLIGHT_CHIP ? EvoStatus.FLIGHT_INSTALLING.getId() : EvoStatus.EVOLVING.getId();
        this.feedbackTicks = 0;
        this.triggerAnim(CONTROLLER_NAME, ANIMATION_USE);
        MicroTechMachineStateHelper.setMachineActive(this, true);
        this.setChanged();
        this.syncClient();
        return true;
    }

    public boolean hasValidSword() {
        return isValidSword(this.getInputStack());
    }

    public boolean hasValidChip() {
        return isValidSword(this.getInputStack())
                ? isValidSwordChip(this.getMaterialStack())
                : isValidFlightChip(this.getMaterialStack());
    }

    public boolean isSwordAtMaxLevel() {
        TechChipType chipType = getSwordChipType(this.getMaterialStack());
        return chipType != null && TechSwordData.getChipLevel(this.getInputStack(), chipType) >= chipType.getMaxLevel();
    }

    private void completeEvolution() {
        this.evolving = false;
        this.evolutionTicks = 0;
        this.useAnimationPlayed = false;
        MicroTechMachineStateHelper.setMachineActive(this, false);

        ItemStack sword = this.getInputStack();
        ItemStack chip = this.getMaterialStack();
        if (this.evolutionMode == EvolutionMode.FLIGHT_CHIP) {
            if (!TechArmorUpgradeHelper.isTechArmorChestplate(sword) || !isValidFlightChip(chip) || TechArmorUpgradeHelper.hasFlightChip(sword)) {
                this.feedbackState = EvoStatus.FLIGHT_INVALID_ITEM.getId();
                this.feedbackTicks = 80;
                this.setChanged();
                this.syncClient();
                this.evolutionMode = EvolutionMode.NONE;
                this.evolutionDuration = SWORD_EVOLUTION_DURATION;
                return;
            }

            TechArmorUpgradeHelper.installFlightChip(sword);
            chip.shrink(1);
            if (chip.isEmpty()) {
                this.items.set(SLOT_MATERIAL, ItemStack.EMPTY);
            }
            this.feedbackState = EvoStatus.FLIGHT_INSTALLED.getId();
            this.feedbackTicks = 80;
            this.evolutionMode = EvolutionMode.NONE;
            this.evolutionDuration = SWORD_EVOLUTION_DURATION;
            this.setChanged();
            this.syncClient();
            return;
        }

        TechChipType chipType = getSwordChipType(chip);
        if (!isValidSword(sword) || chipType == null) {
            this.feedbackState = EvoStatus.INACTIVE.getId();
            this.feedbackTicks = 0;
            this.evolutionMode = EvolutionMode.NONE;
            this.evolutionDuration = SWORD_EVOLUTION_DURATION;
            return;
        }

        int currentLevel = TechSwordData.getChipLevel(sword, chipType);
        if (currentLevel >= chipType.getMaxLevel()) {
            this.feedbackState = EvoStatus.CHIP_MAX_LEVEL.getId();
            this.feedbackTicks = 80;
            this.setChanged();
            this.syncClient();
            this.evolutionMode = EvolutionMode.NONE;
            this.evolutionDuration = SWORD_EVOLUTION_DURATION;
            return;
        }

        if (currentLevel == 0) {
            TechSwordData.setChipLevel(sword, chipType, 1);
            this.feedbackState = EvoStatus.CHIP_INSTALLED.getId();
        } else {
            TechSwordData.setChipLevel(sword, chipType, currentLevel + 1);
            this.feedbackState = EvoStatus.CHIP_UPGRADED.getId();
        }

        if (chipType.isActive()) {
            TechSwordData.addInstalledActiveAbility(sword, chipType.getId());
            if (TechSwordData.getSelectedActiveAbility(sword).isBlank()) {
                TechSwordData.setSelectedActiveAbility(sword, chipType.getId());
            }
        }

        chip.shrink(1);
        if (chip.isEmpty()) {
            this.items.set(SLOT_MATERIAL, ItemStack.EMPTY);
        }

        this.feedbackTicks = 80;
        this.evolutionMode = EvolutionMode.NONE;
        this.evolutionDuration = SWORD_EVOLUTION_DURATION;
        this.setChanged();
        this.syncClient();
    }

    private static boolean isValidSword(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Microtech.TECH_SWORD.get());
    }

    private static boolean isValidChip(ItemStack stack) {
        return isValidSwordChip(stack) || isValidFlightChip(stack);
    }

    private static boolean isValidSwordChip(ItemStack stack) {
        return stack.getItem() instanceof TechChipItem;
    }

    private static boolean isValidFlightChip(ItemStack stack) {
        return stack.is(Microtech.TECH_FLIGHT_CHIP.get());
    }

    private static TechChipType getSwordChipType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TechChipItem chipItem)) {
            return null;
        }

        return chipItem.getChipType();
    }
}
