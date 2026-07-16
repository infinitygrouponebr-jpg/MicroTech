package Infinitygroup.microtech.item;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.client.renderer.item.GraviteRenderer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GraviteItem extends Item implements GeoItem {
    private static final String CONTROLLER_NAME = "gravite_controller";
    private static final String ANIMATION_USE = "use";
    private static final RawAnimation USE_ANIMATION = RawAnimation.begin().thenPlay(ANIMATION_USE);
    private static final int MINING_INTERVAL_TICKS = 10;
    private static final int ANIMATION_REFRESH_TICKS = 12;
    private static final int MESSAGE_THROTTLE_TICKS = 40;
    private static final double RAYCAST_RANGE = 6.0D;
    private static final int LASER_STEPS = 8;
    private static final Map<UUID, Long> LAST_STATUS_MESSAGE_TICK = new HashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GraviteItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            GraviteEnergyHelper.ensureInitialized(stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!GraviteEnergyHelper.isGravite(stack) || player.isSpectator()) {
            return InteractionResultHolder.pass(stack);
        }

        BlockHitResult hitResult = this.getCurrentTarget(level, player, hand);
        if (hitResult == null) {
            return InteractionResultHolder.pass(stack);
        }

        if (!this.canStartChannel(level, player, stack, hitResult)) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            this.playStartEffects(serverLevel, player, stack, hitResult);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSpectator()) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        if (!GraviteEnergyHelper.isGravite(stack)) {
            return InteractionResult.PASS;
        }

        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(context.getClickedPos()),
                context.getClickedFace(),
                context.getClickedPos(),
                false
        );
        if (!this.canStartChannel(context.getLevel(), player, stack, hitResult)) {
            return InteractionResult.PASS;
        }

        player.startUsingItem(context.getHand());
        if (!context.getLevel().isClientSide && context.getLevel() instanceof ServerLevel serverLevel) {
            this.playStartEffects(serverLevel, player, stack, hitResult);
        }

        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }

        if (!GraviteEnergyHelper.isGravite(stack)) {
            player.stopUsingItem();
            return;
        }

        int usedTicks = this.getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (usedTicks <= 0) {
            return;
        }

        BlockHitResult hitResult = this.getCurrentTarget(level, player, player.getUsedItemHand());
        if (hitResult == null) {
            return;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(targetPos);
        if (!this.canMine(level, targetPos, state)) {
            return;
        }

        if (!player.isCreative() && GraviteEnergyHelper.getEnergyStored(stack) < GraviteEnergyHelper.BASE_USE_COST + GraviteEnergyHelper.BLOCK_BREAK_COST) {
            this.sendThrottledStatusMessage(player, level, "message.microtech.gravite.no_energy");
            if (level instanceof ServerLevel serverLevel) {
                this.playFailureEffects(serverLevel, player);
            }
            player.stopUsingItem();
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            this.spawnGraviteLaserParticles(serverLevel, player, hitResult.getLocation(), hitResult.getDirection());
        }

        if (usedTicks % MINING_INTERVAL_TICKS != 0) {
            return;
        }

        this.performMiningCycle(level, player, stack, hitResult, usedTicks);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        LAST_STATUS_MESSAGE_TICK.remove(player.getUUID());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return GraviteEnergyHelper.isGravite(stack) && GraviteEnergyHelper.getEnergyStored(stack) < GraviteEnergyHelper.getMaxEnergyStored(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = GraviteEnergyHelper.getMaxEnergyStored(stack);
        if (max <= 0) {
            return 0;
        }

        return Math.min(13, Math.round(13.0F * GraviteEnergyHelper.getEnergyStored(stack) / (float) max));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int percent = GraviteEnergyHelper.getEnergyPercent(stack);
        if (percent < 25) {
            return 0xFF5C5C;
        }
        if (percent < 60) {
            return 0xFFD85C;
        }
        return 0x53DDFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.gravite.type");
        tooltip.add(Component.translatable(
                "tooltip.microtech.gravite.energy",
                MicroTechTooltipHelper.formatFE(GraviteEnergyHelper.getEnergyStoredForTooltip(stack)),
                MicroTechTooltipHelper.formatFE(GraviteEnergyHelper.getMaxEnergyStored(stack))
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.microtech.gravite.charge",
                MicroTechTooltipHelper.formatPercent(GraviteEnergyHelper.getEnergyStoredForTooltip(stack), GraviteEnergyHelper.getMaxEnergyStored(stack))
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.microtech.gravite.mode").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.gravite.range",
                MicroTechTooltipHelper.formatFE((int) RAYCAST_RANGE)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.gravite.use").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(
                "tooltip.microtech.gravite.cost",
                MicroTechTooltipHelper.formatFE(100),
                MicroTechTooltipHelper.formatFE(50)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.gravite.upgrades").withStyle(ChatFormatting.DARK_GRAY));

        if (MicroTechTooltipHelper.isShiftDown()) {
            tooltip.add(Component.translatable("tooltip.microtech.gravite.shift_1").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.gravite.shift_2").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            MicroTechTooltipHelper.addHoldShiftHint(tooltip);
        }
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

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer = new GraviteRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return this.renderer;
            }
        });
    }

    private boolean canStartChannel(Level level, Player player, ItemStack stack, BlockHitResult hitResult) {
        if (player.isSpectator()) {
            return false;
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        if (!this.canMine(level, targetPos, level.getBlockState(targetPos))) {
            return false;
        }

        if (!player.isCreative() && GraviteEnergyHelper.getEnergyStored(stack) < GraviteEnergyHelper.BASE_USE_COST + GraviteEnergyHelper.BLOCK_BREAK_COST) {
            this.sendThrottledStatusMessage(player, level, "message.microtech.gravite.no_energy");
            if (level instanceof ServerLevel serverLevel) {
                this.playFailureEffects(serverLevel, player);
            }
            return false;
        }

        return true;
    }

    private void performMiningCycle(Level level, Player player, ItemStack stack, BlockHitResult hitResult, int usedTicks) {
        if (player.isSpectator()) {
            return;
        }

        BlockPos center = hitResult.getBlockPos();
        Direction face = hitResult.getDirection();
        List<BlockPos> targets = this.getAreaTargets(center, face);
        List<BlockPos> breakableTargets = new ArrayList<>(targets.size());
        for (BlockPos pos : targets) {
            if (this.canMine(level, pos, level.getBlockState(pos))) {
                breakableTargets.add(pos);
            }
        }

        if (breakableTargets.isEmpty()) {
            return;
        }

        int storedEnergy = player.isCreative() ? Integer.MAX_VALUE : GraviteEnergyHelper.getEnergyStored(stack);
        if (!player.isCreative() && storedEnergy < GraviteEnergyHelper.BASE_USE_COST + GraviteEnergyHelper.BLOCK_BREAK_COST) {
            this.sendThrottledStatusMessage(player, level, "message.microtech.gravite.no_energy");
            if (level instanceof ServerLevel serverLevel) {
                this.playFailureEffects(serverLevel, player);
            }
            player.stopUsingItem();
            return;
        }

        int remainingEnergy = storedEnergy;
        int broken = 0;
        boolean baseCharged = false;

        for (BlockPos pos : breakableTargets) {
            if (!player.isCreative() && remainingEnergy < GraviteEnergyHelper.BLOCK_BREAK_COST) {
                break;
            }

            BlockState state = level.getBlockState(pos);
            if (!this.canMine(level, pos, state)) {
                continue;
            }

            boolean removed = player.isCreative()
                    ? level.removeBlock(pos, false)
                    : level.destroyBlock(pos, true, player);
            if (!removed) {
                continue;
            }

            if (!player.isCreative()) {
                if (!baseCharged) {
                    remainingEnergy -= GraviteEnergyHelper.BASE_USE_COST;
                    baseCharged = true;
                }
                remainingEnergy -= GraviteEnergyHelper.BLOCK_BREAK_COST;
            }

            broken++;
            if (level instanceof ServerLevel serverLevel) {
                this.spawnGraviteBreakParticles(serverLevel, pos);
            }
        }

        if (broken <= 0) {
            if (level instanceof ServerLevel serverLevel) {
                this.playFailureEffects(serverLevel, player);
            }
            return;
        }

        if (!player.isCreative()) {
            GraviteEnergyHelper.setEnergyStored(stack, remainingEnergy);
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, center, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5F, 1.25F);
        }

        this.triggerUseAnimation(level, player, stack, usedTicks);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private void triggerUseAnimation(Level level, Player player, ItemStack stack, int usedTicks) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (usedTicks == 1 || usedTicks % ANIMATION_REFRESH_TICKS == 0) {
            this.triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), CONTROLLER_NAME, ANIMATION_USE);
        }
    }

    private void playStartEffects(ServerLevel level, Player player, ItemStack stack, BlockHitResult hitResult) {
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 1.2F);
        this.triggerAnim(player, GeoItem.getOrAssignId(stack, level), CONTROLLER_NAME, ANIMATION_USE);
        this.spawnGraviteLaserParticles(level, player, hitResult.getLocation(), hitResult.getDirection());
        this.spawnTargetPulse(level, hitResult.getBlockPos());
    }

    private void spawnTargetPulse(ServerLevel level, BlockPos targetPos) {
        Vec3 target = Vec3.atCenterOf(targetPos);
        level.sendParticles(
                new DustParticleOptions(new Vector3f(0.10F, 0.82F, 1.0F), 1.0F),
                target.x,
                target.y + 0.1D,
                target.z,
                8,
                0.12D,
                0.12D,
                0.12D,
                0.0D
        );
    }

    private void spawnGraviteLaserParticles(ServerLevel level, Player player, Vec3 target, Direction face) {
        Vec3 origin = this.getLaserOrigin(player);
        Vec3 delta = target.subtract(origin);
        if (delta.lengthSqr() <= 0.0001D) {
            return;
        }

        for (int step = 0; step <= LASER_STEPS; step++) {
            double progress = step / (double) LASER_STEPS;
            Vec3 point = origin.add(delta.scale(progress));

            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.12F, 0.84F, 1.0F), 1.0F),
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.015D,
                    0.015D,
                    0.015D,
                    0.0D
            );

            if (step % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }

            if (step % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.005D, 0.005D, 0.005D, 0.0D);
            }
        }

        if (face == Direction.UP || face == Direction.DOWN) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.x, target.y, target.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private Vec3 getLaserOrigin(Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        double handOffset = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 0.22D : -0.22D;
        return player.getEyePosition()
                .add(look.scale(0.55D))
                .add(right.scale(handOffset))
                .add(0.0D, -0.12D, 0.0D);
    }

    private void spawnGraviteBreakParticles(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(
                new DustParticleOptions(new Vector3f(0.16F, 0.78F, 1.0F), 1.0F),
                center.x,
                center.y,
                center.z,
                2,
                0.06D,
                0.06D,
                0.06D,
                0.0D
        );
    }

    private void playFailureEffects(ServerLevel level, Player player) {
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.25F, 0.75F);
        level.sendParticles(
                new DustParticleOptions(new Vector3f(1.0F, 0.18F, 0.18F), 1.0F),
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                2,
                0.08D,
                0.08D,
                0.08D,
                0.0D
        );
    }

    private void sendThrottledStatusMessage(Player player, Level level, String key) {
        if (level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        UUID uuid = player.getUUID();
        long lastTick = LAST_STATUS_MESSAGE_TICK.getOrDefault(uuid, Long.MIN_VALUE);
        if (gameTime - lastTick < MESSAGE_THROTTLE_TICKS) {
            return;
        }

        LAST_STATUS_MESSAGE_TICK.put(uuid, gameTime);
        player.displayClientMessage(Component.translatable(key), true);
    }

    private boolean canMine(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.hasBlockEntity()) {
            return false;
        }

        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }

        return state.getFluidState().isEmpty();
    }

    private BlockHitResult getCurrentTarget(Level level, Player player, InteractionHand hand) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        double handOffset = hand == InteractionHand.MAIN_HAND ? 0.22D : -0.22D;
        Vec3 start = player.getEyePosition()
                .add(look.scale(0.55D))
                .add(right.scale(handOffset))
                .add(0.0D, -0.12D, 0.0D);
        Vec3 end = start.add(look.scale(RAYCAST_RANGE));

        HitResult hitResult = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        return (BlockHitResult) hitResult;
    }

    private List<BlockPos> getAreaTargets(BlockPos center, Direction face) {
        List<BlockPos> positions = new ArrayList<>(9);
        for (int first = -1; first <= 1; first++) {
            for (int second = -1; second <= 1; second++) {
                positions.add(switch (face) {
                    case UP, DOWN -> center.offset(first, 0, second);
                    case NORTH, SOUTH -> center.offset(first, second, 0);
                    case EAST, WEST -> center.offset(0, second, first);
                });
            }
        }
        return positions;
    }
}
