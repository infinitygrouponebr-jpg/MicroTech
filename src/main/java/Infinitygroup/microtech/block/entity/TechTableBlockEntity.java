package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.machine.TechTableRecipeHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TechTableBlockEntity extends BlockEntity implements Container, GeoBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;
    private static final String ITEMS_TAG = "Items";
    private static final String STATE_TAG = "State";
    private static final String FEEDBACK_STATE_TAG = "FeedbackState";
    private static final String FEEDBACK_TICKS_TAG = "FeedbackTicks";
    private static final String SESSION_PLAYER_TAG = "SessionPlayer";
    private static final String SESSION_ACTIVE_TAG = "SessionActive";
    private static final String SESSION_TICKS_TAG = "SessionTicks";
    private static final String SESSION_PERFECT_TAG = "SessionPerfectHits";
    private static final String SESSION_GOOD_TAG = "SessionGoodHits";
    private static final String SESSION_HITS_TAG = "SessionHits";
    private static final String SESSION_MISTAKES_TAG = "SessionMistakes";
    private static final String SESSION_INSTABILITY_TAG = "SessionInstability";
    private static final String LAST_HIT_TAG = "LastHitQuality";
    private static final String DISPLAY_SHAKE_TAG = "DisplayShakeTicks";
    private static final String DISPLAY_SHAKE_STRENGTH_TAG = "DisplayShakeStrength";
    private static final String BOSS_VISIBLE_TAG = "BossVisible";
    private static final String CONTROLLER_NAME = "main_controller";
    private static final Map<UUID, TechTableBlockEntity> OWNER_INDEX = new HashMap<>();

    public enum TechTableState {
        IDLE(0, "screen.microtech.tech_table.idle", 0xD0D0D0),
        INVALID_INPUT(1, "screen.microtech.tech_table.invalid_input", 0xFF8A8A),
        BLOCKED(2, "screen.microtech.tech_table.blocked", 0xFF8A8A),
        READY(3, "screen.microtech.tech_table.ready", 0x7DF5A2),
        WORKING(4, "screen.microtech.tech_table.working", 0xF5D36C),
        COMPLETE(5, "screen.microtech.tech_table.complete", 0x6CE7FF),
        FAILED(6, "screen.microtech.tech_table.failed", 0xFF8A8A),
        CANCELLED(7, "screen.microtech.tech_table.cancelled", 0x9CB8FF);

        private final int id;
        private final String translationKey;
        private final int color;

        TechTableState(int id, String translationKey, int color) {
            this.id = id;
            this.translationKey = translationKey;
            this.color = color;
        }

        public int getId() {
            return this.id;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public int getColor() {
            return this.color;
        }

        public Component getText() {
            return Component.translatable(this.translationKey);
        }

        public boolean isWorking() {
            return this == WORKING;
        }

        public static TechTableState fromId(int id) {
            for (TechTableState state : values()) {
                if (state.id == id) {
                    return state;
                }
            }
            return IDLE;
        }
    }

    private static final int FEEDBACK_DURATION = 20;
    private static final int SESSION_TIMEOUT_TICKS = 600;
    private static final int TARGET_BAR_SEGMENTS = 16;
    private static final double CYCLE_SPEED_BASE = 0.22D;
    private static final int MAX_MISTAKES = 5;
    private static final double PERFECT_RANGE_BASE = 0.05D;
    private static final double GOOD_RANGE_BASE = 0.15D;
    private static final double PERFECT_RANGE_MIN = 0.025D;
    private static final double GOOD_RANGE_MIN = 0.08D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.empty(), net.minecraft.world.BossEvent.BossBarColor.YELLOW, net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);

    private TechTableState feedbackState = TechTableState.IDLE;
    private int feedbackTicks;
    private UUID sessionPlayer;
    private boolean sessionActive;
    private int sessionTicks;
    private int sessionPerfectHits;
    private int sessionGoodHits;
    private int sessionHits;
    private int sessionMistakes;
    private int sessionInstability;
    private HitQuality lastHitQuality = HitQuality.GOOD;
    private int displayShakeTicks;
    private float displayShakeStrength;
    private boolean bossVisible;

    public TechTableBlockEntity(BlockPos pos, BlockState state) {
        super(Microtech.TECH_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TechTableBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.isRemoved()) {
            return;
        }

        boolean changed = false;

        if (blockEntity.feedbackTicks > 0) {
            blockEntity.feedbackTicks--;
            changed = true;
        }

        if (blockEntity.displayShakeTicks > 0) {
            blockEntity.displayShakeTicks--;
            changed = true;
            if (blockEntity.displayShakeTicks <= 0) {
                blockEntity.displayShakeStrength = 0.0F;
            }
        }

        if (blockEntity.feedbackTicks <= 0 && !blockEntity.isWorking()) {
            blockEntity.validateIdleState();
        }

        if (blockEntity.isWorking()) {
            blockEntity.sessionTicks++;

            ServerPlayer player = blockEntity.getActivePlayer(level);
            if (player == null || player.isRemoved() || !player.isAlive()) {
                blockEntity.abortSession(level, null, true);
                changed = true;
            } else {
                if (blockEntity.sessionTicks % 2 == 0) {
                    player.displayClientMessage(blockEntity.buildActionBarMessage(), true);
                }

                if (blockEntity.sessionTicks >= SESSION_TIMEOUT_TICKS) {
                    blockEntity.abortSession(level, player, true);
                    changed = true;
                } else {
                    blockEntity.updateBossBar(player);
                }
            }
        } else if (blockEntity.bossVisible) {
            blockEntity.clearBossBar();
            changed = true;
        }

        if (changed) {
            blockEntity.setChanged();
            blockEntity.syncClient();
        }
    }

    public Component getDisplayName() {
        return Component.translatable("container.microtech.tech_table");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.feedbackState = tag.contains(FEEDBACK_STATE_TAG, Tag.TAG_INT) ? TechTableState.fromId(tag.getInt(FEEDBACK_STATE_TAG)) : TechTableState.IDLE;
        this.feedbackTicks = Math.max(0, tag.getInt(FEEDBACK_TICKS_TAG));
        this.sessionTicks = Math.max(0, tag.getInt(SESSION_TICKS_TAG));
        this.sessionPerfectHits = Math.max(0, tag.getInt(SESSION_PERFECT_TAG));
        this.sessionGoodHits = Math.max(0, tag.getInt(SESSION_GOOD_TAG));
        this.sessionHits = Math.max(0, tag.getInt(SESSION_HITS_TAG));
        this.sessionMistakes = Math.max(0, tag.getInt(SESSION_MISTAKES_TAG));
        this.sessionInstability = Math.max(0, tag.getInt(SESSION_INSTABILITY_TAG));
        this.lastHitQuality = HitQuality.fromId(tag.getInt(LAST_HIT_TAG));
        this.displayShakeTicks = Math.max(0, tag.getInt(DISPLAY_SHAKE_TAG));
        this.displayShakeStrength = Mth.clamp(tag.getFloat(DISPLAY_SHAKE_STRENGTH_TAG), 0.0F, 2.0F);
        this.bossVisible = tag.getBoolean(BOSS_VISIBLE_TAG);
        this.sessionPlayer = tag.hasUUID(SESSION_PLAYER_TAG) ? tag.getUUID(SESSION_PLAYER_TAG) : null;
        this.sessionActive = tag.getBoolean(SESSION_ACTIVE_TAG);
        this.feedbackState = this.feedbackTicks > 0 ? this.feedbackState : TechTableState.IDLE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt(FEEDBACK_STATE_TAG, this.feedbackState.getId());
        tag.putInt(FEEDBACK_TICKS_TAG, this.feedbackTicks);
        tag.putInt(SESSION_TICKS_TAG, this.sessionTicks);
        tag.putInt(SESSION_PERFECT_TAG, this.sessionPerfectHits);
        tag.putInt(SESSION_GOOD_TAG, this.sessionGoodHits);
        tag.putInt(SESSION_HITS_TAG, this.sessionHits);
        tag.putInt(SESSION_MISTAKES_TAG, this.sessionMistakes);
        tag.putInt(SESSION_INSTABILITY_TAG, this.sessionInstability);
        tag.putInt(LAST_HIT_TAG, this.lastHitQuality.getId());
        tag.putInt(DISPLAY_SHAKE_TAG, this.displayShakeTicks);
        tag.putFloat(DISPLAY_SHAKE_STRENGTH_TAG, this.displayShakeStrength);
        tag.putBoolean(BOSS_VISIBLE_TAG, this.bossVisible);
        tag.putBoolean(SESSION_ACTIVE_TAG, this.sessionActive);
        if (this.sessionPlayer != null) {
            tag.putUUID(SESSION_PLAYER_TAG, this.sessionPlayer);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt(FEEDBACK_STATE_TAG, this.feedbackState.getId());
        tag.putInt(FEEDBACK_TICKS_TAG, this.feedbackTicks);
        tag.putInt(SESSION_TICKS_TAG, this.sessionTicks);
        tag.putInt(SESSION_PERFECT_TAG, this.sessionPerfectHits);
        tag.putInt(SESSION_GOOD_TAG, this.sessionGoodHits);
        tag.putInt(SESSION_HITS_TAG, this.sessionHits);
        tag.putInt(SESSION_MISTAKES_TAG, this.sessionMistakes);
        tag.putInt(SESSION_INSTABILITY_TAG, this.sessionInstability);
        tag.putInt(LAST_HIT_TAG, this.lastHitQuality.getId());
        tag.putInt(DISPLAY_SHAKE_TAG, this.displayShakeTicks);
        tag.putFloat(DISPLAY_SHAKE_STRENGTH_TAG, this.displayShakeStrength);
        tag.putBoolean(BOSS_VISIBLE_TAG, this.bossVisible);
        tag.putBoolean(SESSION_ACTIVE_TAG, this.sessionActive);
        if (this.sessionPlayer != null) {
            tag.putUUID(SESSION_PLAYER_TAG, this.sessionPlayer);
        }
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
        if (slot == SLOT_INPUT && this.feedbackTicks <= 0 && !this.isWorking()) {
            this.validateIdleState();
        }
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
        this.abortSession(this.level, null, false);
        this.feedbackState = TechTableState.IDLE;
        this.feedbackTicks = 0;
        this.setChanged();
        this.syncClient();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && TechTableRecipeHelper.isValidInput(stack);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER_NAME, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof TechTableBlockEntity blockEntity)) {
            return;
        }

        TechTableState state = blockEntity.getStatus();
        if (state == TechTableState.IDLE || state == TechTableState.INVALID_INPUT) {
            return;
        }

        event.setCanceled(true);
        if (!blockEntity.canPlayerUse(player)) {
            blockEntity.sendActionBar(player, Component.translatable("message.microtech.tech_table.busy"));
            return;
        }

        if (!blockEntity.isWorking()) {
            if (!blockEntity.startSession(player)) {
                return;
            }
        }

        blockEntity.handleHit(player);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof TechTableBlockEntity blockEntity)) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (blockEntity.isWorking() && !blockEntity.isSessionOwner(player)) {
            event.setCanceled(true);
            blockEntity.sendActionBar(player, Component.translatable("message.microtech.tech_table.busy"));
            return;
        }

        if (stack.isEmpty()) {
            if (blockEntity.hasStoredInput()) {
                event.setCanceled(true);
                blockEntity.sendActionBar(player, Component.translatable(blockEntity.isSessionOwner(player)
                        ? "message.microtech.tech_table.hammer_ready"
                        : "message.microtech.tech_table.busy"));
            }
            return;
        }

        event.setCanceled(true);

        if (blockEntity.hasStoredInput()) {
            if (player.isShiftKeyDown() && blockEntity.isSessionOwner(player) && !blockEntity.isWorking()) {
                blockEntity.cancelPreparedInput(level, player);
                return;
            }
            blockEntity.sendActionBar(player, Component.translatable(blockEntity.isSessionOwner(player)
                    ? "message.microtech.tech_table.hammer_ready"
                    : "message.microtech.tech_table.busy"));
            return;
        }

        if (!TechTableRecipeHelper.isValidInput(stack)) {
            blockEntity.sendActionBar(player, Component.translatable("message.microtech.tech_table.invalid_material"));
            return;
        }

        if (!blockEntity.canPlayerReserve(player)) {
            blockEntity.sendActionBar(player, Component.translatable("message.microtech.tech_table.busy"));
            return;
        }

        blockEntity.storeInput(player, stack);
        blockEntity.sendActionBar(player, Component.translatable("message.microtech.tech_table.material_placed"));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        TechTableBlockEntity blockEntity = OWNER_INDEX.remove(player.getUUID());
        if (blockEntity != null) {
            blockEntity.handleOwnerLogout(player);
        }
    }

    public void onRemovedFromWorld(Level level, BlockPos pos) {
        this.releaseOwner();
        this.sessionActive = false;
        this.sessionPlayer = null;
        this.clearBossBar();
        if (!this.isEmpty()) {
            net.minecraft.world.Containers.dropContents(level, pos, this);
        }
    }

    public boolean startSession(ServerPlayer player) {
        if (player == null || this.getStatus() != TechTableState.READY || !this.canPlayerUse(player)) {
            return false;
        }

        this.claimOwner(player);
        this.sessionActive = true;
        this.sessionTicks = 0;
        this.sessionPerfectHits = 0;
        this.sessionGoodHits = 0;
        this.sessionHits = 0;
        this.sessionMistakes = 0;
        this.sessionInstability = 0;
        this.lastHitQuality = HitQuality.GOOD;
        this.displayShakeTicks = 0;
        this.displayShakeStrength = 0.0F;
        this.feedbackTicks = 0;
        this.feedbackState = TechTableState.IDLE;
        this.updateBossBar(player);
        this.setChanged();
        this.syncClient();
        return true;
    }

    public boolean cancelSession(boolean failedFeedback) {
        return this.cancelSession(failedFeedback, true);
    }

    private boolean cancelSession(boolean failedFeedback, boolean returnInput) {
        if (!this.isWorking() && this.sessionPlayer == null) {
            return false;
        }

        if (failedFeedback) {
            this.feedbackState = TechTableState.CANCELLED;
            this.feedbackTicks = FEEDBACK_DURATION;
        }

        this.sessionActive = false;
        this.sessionTicks = 0;
        this.sessionPerfectHits = 0;
        this.sessionGoodHits = 0;
        this.sessionHits = 0;
        this.sessionMistakes = 0;
        this.sessionInstability = 0;
        this.lastHitQuality = HitQuality.GOOD;
        this.displayShakeTicks = 0;
        this.displayShakeStrength = 0.0F;
        this.clearBossBar();
        this.releaseOwner();
        if (returnInput && !this.getItem(SLOT_INPUT).isEmpty() && this.level instanceof ServerLevel serverLevel) {
            ServerPlayer owner = this.getActivePlayer(serverLevel);
            this.ejectStoredInput(serverLevel, owner);
        }
        this.setChanged();
        this.syncClient();
        return true;
    }

    public boolean handleHit(ServerPlayer player) {
        if (player == null || !this.canPlayerUse(player)) {
            return false;
        }

        if (!this.isWorking()) {
            if (!this.isSessionOwner(player) && this.sessionPlayer != null) {
                return false;
            }
            if (!this.startSession(player)) {
                return false;
            }
        }

        TechTableRecipeHelper.TechTableRecipe recipe = this.getRecipe();
        if (recipe == null) {
            if (player.level() instanceof ServerLevel serverLevel) {
                this.failSession(serverLevel, player, "invalid_recipe");
            } else {
                this.abortSession(player.level(), player, true);
            }
            return false;
        }

        double cursor = this.getCursorPosition();
        double distance = Math.abs(cursor - 0.5D);
        HitQuality quality;
        double perfectWindow = this.getDynamicPerfectWindow();
        double goodWindow = this.getDynamicGoodWindow();
        if (distance <= perfectWindow) {
            quality = HitQuality.PERFECT;
        } else if (distance <= goodWindow) {
            quality = HitQuality.GOOD;
        } else {
            quality = HitQuality.MISTAKE;
        }

        this.lastHitQuality = quality;
        this.displayShakeTicks = quality == HitQuality.MISTAKE ? 10 : 7;
        this.displayShakeStrength = switch (quality) {
            case PERFECT -> 1.15F;
            case GOOD -> 0.75F;
            case MISTAKE -> 0.50F;
        };

        if (player.level() instanceof ServerLevel serverLevel) {
            switch (quality) {
                case PERFECT -> {
                    serverLevel.playSound(null, this.worldPosition, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 1.0F, 1.22F);
                    this.spawnPerfectParticles(serverLevel, 4);
                }
                case GOOD -> {
                    serverLevel.playSound(null, this.worldPosition, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.9F, 0.98F);
                    this.spawnGoodParticles(serverLevel, 3);
                }
                case MISTAKE -> {
                    serverLevel.playSound(null, this.worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.65F, 0.72F);
                    this.spawnMistakeParticles(serverLevel, 3);
                }
            }
        }

        switch (quality) {
            case PERFECT -> {
                this.sessionPerfectHits++;
                this.sessionHits = Math.min(recipe.requiredHits(), this.sessionHits + 2);
                this.sessionInstability = Math.max(0, this.sessionInstability - 1);
            }
            case GOOD -> {
                this.sessionGoodHits++;
                this.sessionHits = Math.min(recipe.requiredHits(), this.sessionHits + 1);
            }
            case MISTAKE -> {
                this.sessionMistakes++;
                this.sessionHits = Math.max(0, this.sessionHits - 1);
                this.sessionInstability = Math.min(20, this.sessionInstability + 1);
                if (this.sessionMistakes >= MAX_MISTAKES) {
                    this.failSession((ServerLevel) player.level(), player, "mistakes");
                    return true;
                }
            }
        }

        if (this.sessionHits >= recipe.requiredHits()) {
            this.completeSession((ServerLevel) player.level());
            return true;
        }

        this.setChanged();
        this.syncClient();
        this.updateBossBar(player);
        return true;
    }

    public TechTableState getStatus() {
        if (this.feedbackTicks > 0) {
            return this.feedbackState;
        }
        if (this.isWorking()) {
            return TechTableState.WORKING;
        }
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return TechTableState.IDLE;
        }

        TechTableRecipeHelper.TechTableRecipe recipe = this.getRecipe();
        if (recipe == null) {
            return TechTableState.INVALID_INPUT;
        }

        if (!this.canAcceptOutput(recipe.createOutputStack())) {
            return TechTableState.BLOCKED;
        }

        return TechTableState.READY;
    }

    public ItemStack getDisplayStack() {
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return TechTableRecipeHelper.isValidInput(input) ? input.copy() : ItemStack.EMPTY;
    }

    public int getRequiredHits() {
        TechTableRecipeHelper.TechTableRecipe recipe = this.getRecipe();
        return recipe != null ? recipe.requiredHits() : 0;
    }

    public int getSessionPerfectHits() {
        return this.sessionPerfectHits;
    }

    public int getSessionGoodHits() {
        return this.sessionGoodHits;
    }

    public int getSessionHits() {
        return this.sessionHits;
    }

    public int getSessionMistakes() {
        return this.sessionMistakes;
    }

    public int getSessionInstability() {
        return this.sessionInstability;
    }

    public HitQuality getLastHitQuality() {
        return this.lastHitQuality;
    }

    public int getLastHitQualityId() {
        return this.lastHitQuality.getId();
    }

    public int getSessionTicks() {
        return this.sessionTicks;
    }

    public int getDisplayShakeTicks() {
        return this.displayShakeTicks;
    }

    public float getDisplayShakeStrength() {
        return this.displayShakeStrength;
    }

    public Component getRecipeDisplayName() {
        TechTableRecipeHelper.TechTableRecipe recipe = this.getRecipe();
        return recipe != null ? recipe.getOutputDisplayName() : Component.translatable("screen.microtech.tech_table.no_recipe");
    }

    public Component getInputDisplayName() {
        TechTableRecipeHelper.TechTableRecipe recipe = this.getRecipe();
        return recipe != null ? recipe.getInputDisplayName() : Component.translatable("screen.microtech.tech_table.no_recipe");
    }

    public String getCursorBar() {
        double cursor = this.getCursorPosition();
        int segments = TARGET_BAR_SEGMENTS;
        int cursorIndex = Mth.clamp((int) Math.round(cursor * (segments - 1)), 0, segments - 1);
        double goodWindow = this.getDynamicGoodWindow();
        double perfectWindow = this.getDynamicPerfectWindow();
        int goodStart = Mth.clamp((int) Math.floor((0.5D - goodWindow) * segments), 0, segments - 1);
        int goodEnd = Mth.clamp((int) Math.ceil((0.5D + goodWindow) * segments), 0, segments - 1);
        int perfectStart = Mth.clamp((int) Math.floor((0.5D - perfectWindow) * segments), 0, segments - 1);
        int perfectEnd = Mth.clamp((int) Math.ceil((0.5D + perfectWindow) * segments), 0, segments - 1);

        StringBuilder builder = new StringBuilder(segments + 2);
        builder.append('[');
        for (int i = 0; i < segments; i++) {
            if (i == cursorIndex) {
                builder.append('|');
            } else if (i >= perfectStart && i <= perfectEnd) {
                builder.append('=');
            } else if (i >= goodStart && i <= goodEnd) {
                builder.append('-');
            } else {
                builder.append(' ');
            }
        }
        builder.append(']');
        return builder.toString();
    }

    public Component buildActionBarMessage() {
        TechTableRecipeHelper.TechTableRecipe recipe = this.getRecipe();
        Component recipeName = recipe == null ? Component.literal("?") : recipe.getOutputDisplayName();
        Component feedback = switch (this.lastHitQuality) {
            case PERFECT -> Component.translatable("message.microtech.tech_table.perfect");
            case GOOD -> Component.translatable("message.microtech.tech_table.good");
            case MISTAKE -> Component.translatable("message.microtech.tech_table.miss");
        };
        return Component.translatable(
                "screen.microtech.tech_table.action_bar",
                recipeName,
                this.getCursorBar(),
                feedback,
                this.sessionHits,
                this.getRequiredHits(),
                this.sessionMistakes,
                MAX_MISTAKES
        );
    }

    public boolean isWorking() {
        return this.sessionActive;
    }

    public boolean isSessionOwner(Player player) {
        return player != null && this.sessionPlayer != null && this.sessionPlayer.equals(player.getUUID());
    }

    public boolean hasBossBar() {
        return this.bossVisible;
    }

    private void completeSession(ServerLevel serverLevel) {
        TechTableRecipeHelper.TechTableRecipe recipe = this.getRecipe();
        if (recipe == null) {
            this.failSession(serverLevel, null, "invalid_recipe");
            return;
        }

        ItemStack output = recipe.createOutputStack();
        ServerPlayer owner = this.getActivePlayer(serverLevel);
        int perfectHits = this.sessionPerfectHits;
        int goodHits = this.sessionGoodHits;
        int mistakes = this.sessionMistakes;
        this.items.set(SLOT_INPUT, ItemStack.EMPTY);
        this.items.set(SLOT_OUTPUT, ItemStack.EMPTY);
        this.feedbackState = TechTableState.COMPLETE;
        this.feedbackTicks = FEEDBACK_DURATION;
        this.sessionActive = false;
        this.sessionTicks = 0;
        this.sessionPerfectHits = 0;
        this.sessionGoodHits = 0;
        this.sessionHits = 0;
        this.sessionMistakes = 0;
        this.sessionInstability = 0;
        this.lastHitQuality = HitQuality.GOOD;
        this.displayShakeTicks = 0;
        this.displayShakeStrength = 0.0F;
        this.clearBossBar();
        this.releaseOwner();

        this.giveOrDropOutput(serverLevel, owner, output);
        serverLevel.playSound(null, this.worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.9F, 1.0F);
        this.spawnPerfectParticles(serverLevel, 6);
        if (owner != null) {
            owner.displayClientMessage(Component.translatable(
                    mistakes == 0 && perfectHits >= Math.max(2, recipe.requiredHits() - 1)
                            ? "message.microtech.tech_table.perfect_piece"
                            : mistakes <= 2
                                    ? "message.microtech.tech_table.complete_piece"
                                    : "message.microtech.tech_table.unstable_piece"
            ), true);
        }
        this.validateIdleState();
        this.setChanged();
        this.syncClient();
    }

    private double getDynamicPerfectWindow() {
        double reduction = this.sessionMistakes * 0.003D;
        return Math.max(PERFECT_RANGE_MIN, PERFECT_RANGE_BASE - reduction);
    }

    private double getDynamicGoodWindow() {
        double reduction = this.sessionMistakes * 0.006D;
        return Math.max(GOOD_RANGE_MIN, GOOD_RANGE_BASE - reduction);
    }

    private void abortSession(Level level, ServerPlayer player, boolean feedback) {
        if (feedback) {
            this.feedbackState = TechTableState.CANCELLED;
            this.feedbackTicks = FEEDBACK_DURATION;
        }
        this.sessionActive = false;
        this.sessionTicks = 0;
        this.sessionPerfectHits = 0;
        this.sessionGoodHits = 0;
        this.sessionHits = 0;
        this.sessionMistakes = 0;
        this.sessionInstability = 0;
        this.lastHitQuality = HitQuality.GOOD;
        this.displayShakeTicks = 0;
        this.displayShakeStrength = 0.0F;
        this.clearBossBar();
        this.releaseOwner();
        if (!this.getItem(SLOT_INPUT).isEmpty() && level instanceof ServerLevel serverLevel) {
            this.ejectStoredInput(serverLevel, player);
        }
        this.validateIdleState();
        this.setChanged();
        this.syncClient();
    }

    private void failSession(ServerLevel serverLevel, ServerPlayer player, String reason) {
        this.feedbackState = TechTableState.FAILED;
        this.feedbackTicks = FEEDBACK_DURATION;
        this.sessionActive = false;
        this.sessionTicks = 0;
        this.sessionPerfectHits = 0;
        this.sessionGoodHits = 0;
        this.sessionHits = 0;
        this.sessionMistakes = 0;
        this.sessionInstability = 0;
        this.lastHitQuality = HitQuality.MISTAKE;
        this.displayShakeTicks = 0;
        this.displayShakeStrength = 0.0F;
        this.clearBossBar();
        this.releaseOwner();
        this.items.set(SLOT_INPUT, ItemStack.EMPTY);
        this.items.set(SLOT_OUTPUT, ItemStack.EMPTY);
        this.setChanged();
        this.syncClient();
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.microtech.tech_table.failed_material_lost"), true);
        }
        serverLevel.playSound(null, this.worldPosition, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
        this.spawnMistakeParticles(serverLevel, 5);
        this.validateIdleState();
    }

    private void handleOwnerLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }

        if (this.isWorking() || this.isSessionOwner(player) || this.hasStoredInput()) {
            this.abortSession(this.level, player, true);
        }
    }

    private void cancelPreparedInput(Level level, ServerPlayer player) {
        if (player == null || this.isWorking() || !this.isSessionOwner(player)) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            this.ejectStoredInput(serverLevel, player);
            this.sessionActive = false;
            this.sessionTicks = 0;
            this.sessionPerfectHits = 0;
            this.sessionGoodHits = 0;
            this.sessionHits = 0;
            this.sessionMistakes = 0;
            this.sessionInstability = 0;
            this.lastHitQuality = HitQuality.GOOD;
            this.feedbackState = TechTableState.CANCELLED;
            this.feedbackTicks = FEEDBACK_DURATION;
            this.releaseOwner();
            this.setChanged();
            this.syncClient();
            serverLevel.playSound(null, this.worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.0F);
            this.spawnMistakeParticles(serverLevel, 3);
        }
    }

    private void ejectStoredInput(ServerLevel serverLevel, ServerPlayer player) {
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return;
        }

        this.items.set(SLOT_INPUT, ItemStack.EMPTY);
        ItemStack copy = input.copy();
        boolean returned = false;
        if (player != null) {
            returned = player.getInventory().add(copy);
        }

        if (!returned || !copy.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(serverLevel, this.worldPosition.getX(), this.worldPosition.getY() + 0.5D, this.worldPosition.getZ(), copy);
        }
    }

    private void giveOrDropOutput(ServerLevel serverLevel, ServerPlayer player, ItemStack output) {
        if (output.isEmpty()) {
            return;
        }

        ItemStack copy = output.copy();
        boolean returned = false;
        if (player != null) {
            returned = player.getInventory().add(copy);
        }

        if (!returned || !copy.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(serverLevel, this.worldPosition.getX(), this.worldPosition.getY() + 0.5D, this.worldPosition.getZ(), copy);
        }
    }

    private boolean canPlayerReserve(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        TechTableBlockEntity reserved = OWNER_INDEX.get(player.getUUID());
        return reserved == null || reserved == this;
    }

    private boolean canPlayerUse(ServerPlayer player) {
        return player != null && (this.sessionPlayer == null || this.sessionPlayer.equals(player.getUUID()));
    }

    private boolean hasStoredInput() {
        return !this.getItem(SLOT_INPUT).isEmpty();
    }

    private void claimOwner(ServerPlayer player) {
        if (this.sessionPlayer != null && !this.sessionPlayer.equals(player.getUUID())) {
            OWNER_INDEX.remove(this.sessionPlayer);
        }
        this.sessionPlayer = player.getUUID();
        OWNER_INDEX.put(this.sessionPlayer, this);
    }

    private void releaseOwner() {
        if (this.sessionPlayer != null && OWNER_INDEX.get(this.sessionPlayer) == this) {
            OWNER_INDEX.remove(this.sessionPlayer);
        }
        this.sessionPlayer = null;
    }

    private void storeInput(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !TechTableRecipeHelper.isValidInput(stack)) {
            return;
        }

        if (!this.canPlayerReserve(player)) {
            return;
        }

        ItemStack inserted = stack.copyWithCount(1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.setItem(SLOT_INPUT, inserted);
        this.claimOwner(player);
        this.sessionActive = false;
        this.sessionTicks = 0;
        this.sessionHits = 0;
        this.sessionMistakes = 0;
        this.displayShakeTicks = 0;
        this.displayShakeStrength = 0.0F;
        this.feedbackTicks = 0;
        this.feedbackState = TechTableState.IDLE;
        this.setChanged();
        this.syncClient();
    }

    private void sendActionBar(ServerPlayer player, Component message) {
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }

    private void validateIdleState() {
        if (this.feedbackTicks > 0) {
            return;
        }
        TechTableState current = this.getStatus();
        if (current == TechTableState.BLOCKED && this.bossVisible) {
            this.clearBossBar();
        }
    }

    private TechTableRecipeHelper.TechTableRecipe getRecipe() {
        return TechTableRecipeHelper.getRecipe(this.items.get(SLOT_INPUT)).orElse(null);
    }

    private boolean canAcceptOutput(ItemStack output) {
        ItemStack current = this.items.get(SLOT_OUTPUT);
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, output) && current.getCount() < current.getMaxStackSize();
    }

    private double getCursorPosition() {
        TechTableRecipeHelper.TechTableRecipe recipe = this.getRecipe();
        double speed = recipe == null ? CYCLE_SPEED_BASE : recipe.cursorSpeed();
        double wave = Math.sin((this.sessionTicks * speed) + (this.worldPosition.getX() * 0.17D) + (this.worldPosition.getZ() * 0.13D));
        return Mth.clamp(0.5D + wave * 0.42D, 0.0D, 1.0D);
    }

    private ServerPlayer getActivePlayer(Level level) {
        if (this.sessionPlayer == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(this.sessionPlayer);
    }

    private void updateBossBar(ServerPlayer player) {
        if (!this.bossVisible) {
            this.bossVisible = true;
            this.bossEvent.addPlayer(player);
        }
        TechTableRecipeHelper.TechTableRecipe recipe = this.getRecipe();
        Component name = recipe == null ? Component.literal("Tech Table") : recipe.getOutputDisplayName();
        this.bossEvent.setName(Component.translatable(
                "screen.microtech.tech_table.bossbar",
                name,
                this.sessionHits,
                this.getRequiredHits()
        ));
        this.bossEvent.setProgress(this.getRequiredHits() <= 0 ? 0.0F : Mth.clamp((float) this.sessionHits / (float) this.getRequiredHits(), 0.0F, 1.0F));
        this.bossEvent.setColor(net.minecraft.world.BossEvent.BossBarColor.YELLOW);
        this.bossEvent.setOverlay(net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossEvent.setVisible(true);
    }

    private void clearBossBar() {
        this.bossEvent.removeAllPlayers();
        this.bossEvent.setVisible(false);
        this.bossVisible = false;
    }

    private void spawnPerfectParticles(ServerLevel serverLevel, int count) {
        Vec3 center = Vec3.atCenterOf(this.worldPosition).add(0.0D, 1.05D, 0.0D);
        for (int i = 0; i < count; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5D) * 0.24D;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5D) * 0.24D;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x + offsetX, center.y + 0.02D, center.z + offsetZ, 1, 0.0D, 0.015D, 0.0D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.CRIT, center.x + offsetX, center.y + 0.01D, center.z + offsetZ, 1, 0.0D, 0.01D, 0.0D, 0.0D);
        }
    }

    private void spawnGoodParticles(ServerLevel serverLevel, int count) {
        Vec3 center = Vec3.atCenterOf(this.worldPosition).add(0.0D, 1.03D, 0.0D);
        for (int i = 0; i < count; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5D) * 0.20D;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5D) * 0.20D;
            serverLevel.sendParticles(ParticleTypes.CRIT, center.x + offsetX, center.y, center.z + offsetZ, 1, 0.0D, 0.01D, 0.0D, 0.0D);
        }
    }

    private void spawnMistakeParticles(ServerLevel serverLevel, int count) {
        Vec3 center = Vec3.atCenterOf(this.worldPosition).add(0.0D, 1.02D, 0.0D);
        for (int i = 0; i < count; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5D) * 0.22D;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5D) * 0.22D;
            serverLevel.sendParticles(ParticleTypes.SMOKE, center.x + offsetX, center.y, center.z + offsetZ, 1, 0.0D, 0.005D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CRIT, center.x + offsetX, center.y + 0.02D, center.z + offsetZ, 1, 0.0D, 0.005D, 0.0D, 0.0D);
        }
    }

    private void syncClient() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void cancelSession() {
        this.cancelSession(false);
    }

    private enum HitQuality {
        PERFECT(0),
        GOOD(1),
        MISTAKE(2);

        private final int id;

        HitQuality(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public static HitQuality fromId(int id) {
            for (HitQuality value : values()) {
                if (value.id == id) {
                    return value;
                }
            }
            return GOOD;
        }
    }
}
