package Infinitygroup.microtech.item;

import Infinitygroup.microtech.Microtech;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class TechSwordItem extends Item implements GeoItem {
    private static final int SHOCK_DISCHARGE_COOLDOWN_TICKS = 40;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TechSwordItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
        GeoItem.registerSyncedAnimatable(this);
    }

    public static void onLivingHurt(LivingDamageEvent.Pre event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack stack = attacker.getMainHandItem();
        if (stack.isEmpty() || stack.getItem() != Microtech.TECH_SWORD.get()) {
            return;
        }

        float currentAttackDamage = getCurrentAttackDamage(stack, event.getNewDamage(), attacker.level().getGameTime());
        if (event.getNewDamage() < currentAttackDamage) {
            event.setNewDamage(currentAttackDamage);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide) {
            return;
        }

        int cooldown = TechSwordData.getShockDischargeCooldown(stack);
        if (cooldown > 0) {
            TechSwordData.setShockDischargeCooldown(stack, cooldown - 1);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        SwordEvolutionTier tier = TechSwordData.getEvolutionTier(stack);
        int storedEnergy = TechSwordData.getEnergyStored(stack);
        int baseCost = tier.getEnergyCost();

        if (storedEnergy < baseCost) {
            if (attacker instanceof Player player && !player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.microtech.tech_sword.insufficient_energy"), true);
            }
            return true;
        }

        int remainingEnergy = storedEnergy - baseCost;
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.55F, 1.2F);
        }

        remainingEnergy = this.applyEnergyCut(stack, target, remainingEnergy);
        TechSwordData.setEnergyStored(stack, remainingEnergy);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.getItem() != Microtech.TECH_SWORD.get()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!this.tryStartOverloadChannel(level, player, stack)) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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

        if (!this.isOverloadSelected(stack)) {
            player.stopUsingItem();
            return;
        }

        int overloadLevel = TechSwordData.getOverloadLevel(stack);
        if (overloadLevel <= 0 || !TechSwordData.getInstalledActiveAbilities(stack).contains(TechChipType.OVERLOAD.getId())) {
            player.stopUsingItem();
            return;
        }

        if (TechSwordData.isOverloadOnCooldown(stack, level.getGameTime())) {
            player.displayClientMessage(Component.translatable("message.microtech.tech_sword.overload_on_cooldown"), true);
            player.stopUsingItem();
            return;
        }

        int usedTicks = this.getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (usedTicks <= 0) {
            return;
        }

        int chargeTicks = Math.min(usedTicks, TechSwordOverloadBeam.getMaxChargeTicks());

        if (chargeTicks < TechSwordOverloadBeam.getMaxChargeTicks()) {
            int perTickCost = TechSwordOverloadBeam.getPerTickEnergyCost(overloadLevel);
            int currentEnergy = TechSwordData.getEnergyStored(stack);
            if (currentEnergy < perTickCost) {
                player.displayClientMessage(Component.translatable("message.microtech.tech_sword.overload_insufficient_energy"), true);
                player.stopUsingItem();
                return;
            }

            TechSwordData.setEnergyStored(stack, currentEnergy - perTickCost);
        }

        if (chargeTicks % 4 == 0 && level instanceof ServerLevel serverLevel) {
            spawnOverloadChannelParticles(serverLevel, player, chargeTicks, overloadLevel);
        }
        if (chargeTicks == TechSwordOverloadBeam.getMinChargeTicks()) {
            player.displayClientMessage(Component.translatable("message.microtech.tech_sword.overload_ready"), true);
        } else if (chargeTicks % 5 == 0) {
            int percent = Math.round(TechSwordOverloadBeam.getChargePercent(chargeTicks) * 100.0F);
            player.displayClientMessage(Component.translatable("message.microtech.tech_sword.overload_channeling", percent), true);
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }

        if (!this.isOverloadSelected(stack)) {
            return;
        }

        int overloadLevel = TechSwordData.getOverloadLevel(stack);
        if (overloadLevel <= 0 || !TechSwordData.getInstalledActiveAbilities(stack).contains(TechChipType.OVERLOAD.getId())) {
            return;
        }

        int usedTicks = this.getUseDuration(stack, livingEntity) - timeLeft;
        if (usedTicks < TechSwordOverloadBeam.getMinChargeTicks()) {
            return;
        }

        if (TechSwordData.isOverloadOnCooldown(stack, level.getGameTime())) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            this.fireOverloadBeam(serverLevel, player, stack, overloadLevel, Math.min(usedTicks, TechSwordOverloadBeam.getMaxChargeTicks()));
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack stack = attacker.getMainHandItem();
        if (stack.isEmpty() || stack.getItem() != Microtech.TECH_SWORD.get()) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int level = TechSwordData.getShockDischargeLevel(stack);
        if (level <= 0) {
            return;
        }

        int cooldown = TechSwordData.getShockDischargeCooldown(stack);
        if (cooldown > 0) {
            return;
        }

        int currentEnergy = TechSwordData.getEnergyStored(stack);
        int chipCost = TechChipType.SHOCK_DISCHARGE.getEnergyCost(level);
        if (currentEnergy < chipCost) {
            return;
        }

        float finalAttackDamage = event.getNewDamage();
        if (finalAttackDamage <= 0.0F) {
            return;
        }

        double radius = TechChipType.SHOCK_DISCHARGE.getRadius(level);
        double percent = TechChipType.SHOCK_DISCHARGE.getDamagePercent(level);
        float shockDamage = (float) (finalAttackDamage * percent);
        if (shockDamage <= 0.0F) {
            return;
        }

        AABB area = target.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, area, entity ->
                entity.isAlive() && entity != target && entity != attacker);
        if (nearby.isEmpty()) {
            return;
        }

        boolean applied = false;
        for (LivingEntity nearbyTarget : nearby) {
            if (nearbyTarget.hurt(serverLevel.damageSources().generic(), shockDamage)) {
                applied = true;
                spawnShockParticles(serverLevel, target, nearbyTarget);
            }
        }

        if (!applied) {
            return;
        }

        TechSwordData.setShockDischargeCooldown(stack, SHOCK_DISCHARGE_COOLDOWN_TICKS);
        TechSwordData.setEnergyStored(stack, currentEnergy - chipCost);

        serverLevel.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.45F, 1.35F);
        if (attacker instanceof Player player) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SwordEvolutionTier tier = TechSwordData.getEvolutionTier(stack);
        int energy = TechSwordData.getEnergyStored(stack);
        boolean charged = energy >= tier.getEnergyCost();

        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_sword.energy",
                formatEnergy(energy),
                formatEnergy(tier.getEnergyCapacity())
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_sword.status",
                Component.translatable(charged
                        ? "tooltip.microtech.tech_sword.charged"
                        : "tooltip.microtech.tech_sword.no_energy")
        ).withStyle(charged ? ChatFormatting.GREEN : ChatFormatting.RED));

        tooltip.add(Component.translatable("tooltip.microtech.tech_sword.chips_title").withStyle(ChatFormatting.AQUA));
        int energyCutLevel = TechSwordData.getEnergyCutLevel(stack);
        int shockLevel = TechSwordData.getShockDischargeLevel(stack);
        int overloadLevel = TechSwordData.getOverloadLevel(stack);
        if (energyCutLevel > 0) {
            tooltip.add(Component.translatable("tooltip.microtech.tech_sword.energy_cut_level", energyCutLevel).withStyle(ChatFormatting.WHITE));
        }
        if (shockLevel > 0) {
            tooltip.add(Component.translatable("tooltip.microtech.tech_sword.shock_discharge_level", shockLevel).withStyle(ChatFormatting.WHITE));
        }
        if (overloadLevel > 0) {
            tooltip.add(Component.translatable("tooltip.microtech.tech_sword.overload_level", overloadLevel).withStyle(ChatFormatting.WHITE));
        }
        if (energyCutLevel == 0 && shockLevel == 0 && overloadLevel == 0) {
            tooltip.add(Component.translatable("tooltip.microtech.tech_sword.chip_none").withStyle(ChatFormatting.GRAY));
        }

        String selectedActiveAbility = TechSwordData.getSelectedActiveAbility(stack);
        if (!selectedActiveAbility.isBlank()) {
            tooltip.add(Component.translatable(
                    "tooltip.microtech.tech_sword.active_ability",
                    this.getAbilityDisplayName(selectedActiveAbility)
            ).withStyle(ChatFormatting.AQUA));

            if (TechChipType.OVERLOAD.getId().equals(selectedActiveAbility) && overloadLevel > 0) {
                tooltip.add(Component.translatable("tooltip.microtech.tech_sword.overload_hint_hold").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.microtech.tech_sword.overload_hint_release").withStyle(ChatFormatting.GRAY));
            }
        }

        tooltip.add(Component.translatable("tooltip.microtech.tech_sword.lore_1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.tech_sword.lore_2").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer = new Infinitygroup.microtech.client.renderer.item.TechSwordRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return this.renderer;
            }
        });
    }

    private int applyEnergyCut(ItemStack stack, LivingEntity target, int availableEnergy) {
        int level = TechSwordData.getEnergyCutLevel(stack);
        if (level <= 0) {
            return availableEnergy;
        }

        int chipCost = TechChipType.ENERGY_CUT.getEnergyCost(level);
        if (availableEnergy < chipCost) {
            return availableEnergy;
        }

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(0.18F, 0.82F, 1.0F), 1.0F),
                    target.getX(),
                    target.getY(0.5D),
                    target.getZ(),
                    6,
                    0.15D,
                    0.15D,
                    0.15D,
                    0.0D
            );
        }

        return availableEnergy - chipCost;
    }

    private static void spawnShockParticles(ServerLevel level, LivingEntity source, LivingEntity target) {
        Vec3 from = source.getEyePosition();
        Vec3 to = target.getEyePosition();
        for (int step = 0; step <= 4; step++) {
            double progress = step / 4.0D;
            double x = Mth.lerp(progress, from.x, to.x);
            double y = Mth.lerp(progress, from.y, to.y);
            double z = Mth.lerp(progress, from.z, to.z);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
        level.sendParticles(new DustParticleOptions(new Vector3f(0.2F, 0.8F, 1.0F), 1.0F),
                target.getX(), target.getY(0.5D), target.getZ(), 2, 0.12D, 0.12D, 0.12D, 0.0D);
    }

    public static float getCurrentAttackDamage(ItemStack stack, float incomingDamage) {
        return getCurrentAttackDamage(stack, incomingDamage, Long.MAX_VALUE);
    }

    public static float getCurrentAttackDamage(ItemStack stack, float incomingDamage, long gameTime) {
        SwordEvolutionTier tier = TechSwordData.getEvolutionTier(stack);
        int storedEnergy = TechSwordData.getEnergyStored(stack);
        int baseEnergyCost = tier.getEnergyCost();
        boolean charged = storedEnergy >= baseEnergyCost;

        float currentDamage = Math.max(incomingDamage, charged ? tier.getChargedDamage() : tier.getUnchargedDamage());
        if (charged) {
            int energyCutLevel = TechSwordData.getEnergyCutLevel(stack);
            if (energyCutLevel > 0) {
                int chipCost = TechChipType.ENERGY_CUT.getEnergyCost(energyCutLevel);
                if (storedEnergy >= baseEnergyCost + chipCost) {
                    currentDamage = Math.max(currentDamage, tier.getChargedDamage() + TechChipType.ENERGY_CUT.getDamageBonus(energyCutLevel));
                }
            }
        }

        return currentDamage;
    }

    private static void spawnOverloadChannelParticles(ServerLevel level, Player player, int usedTicks, int overloadLevel) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 focus = eye.add(look.scale(0.65D));
        int sparkCount = 2 + overloadLevel;
        if (usedTicks % 8 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.12F, 1.7F);
        }

        level.sendParticles(
                new DustParticleOptions(new Vector3f(0.16F, 0.75F, 1.0F), 1.0F),
                focus.x,
                focus.y,
                focus.z,
                sparkCount,
                0.05D,
                0.05D,
                0.05D,
                0.0D
        );
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, focus.x, focus.y, focus.z, 1 + overloadLevel / 2, 0.04D, 0.04D, 0.04D, 0.0D);
    }

    private static String formatEnergy(int energy) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(energy);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        TechSwordOverloadBeam.tick(server);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TechSwordOverloadBeam.clear(event.getEntity().getUUID());
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            TechSwordOverloadBeam.clear(event.getOriginal().getUUID());
        }
    }

    private boolean tryStartOverloadChannel(Level level, Player player, ItemStack stack) {
        if (!this.isOverloadSelected(stack)) {
            return false;
        }

        int overloadLevel = TechSwordData.getOverloadLevel(stack);
        if (overloadLevel <= 0 || !TechSwordData.getInstalledActiveAbilities(stack).contains(TechChipType.OVERLOAD.getId())) {
            return false;
        }

        return true;
    }

    private boolean isOverloadSelected(ItemStack stack) {
        return TechChipType.OVERLOAD.getId().equals(TechSwordData.getSelectedActiveAbility(stack));
    }

    private void fireOverloadBeam(ServerLevel serverLevel, Player player, ItemStack stack, int overloadLevel, int usedTicks) {
        float chargePercent = TechSwordOverloadBeam.getChargePercent(usedTicks);
        float damage = TechSwordOverloadBeam.getDamage(overloadLevel, usedTicks);
        double range = TechSwordOverloadBeam.getRange(overloadLevel, usedTicks);
        int visualTicks = TechSwordOverloadBeam.getVisualTicks(overloadLevel, usedTicks);
        int maxTargets = TechSwordOverloadBeam.getMaxTargets(overloadLevel);

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(range));

        BlockHitResult blockHit = serverLevel.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        List<LivingEntity> targets = this.findBeamTargets(serverLevel, player, start, end, maxTargets);
        for (LivingEntity target : targets) {
            target.hurt(serverLevel.damageSources().magic(), damage);
        }

        TechSwordData.setOverloadCooldownUntil(stack, serverLevel.getGameTime() + TechChipType.OVERLOAD.getCooldownTicks(overloadLevel));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.70F, 1.25F);
        for (LivingEntity target : targets) {
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.85F, 1.0F);
        }

        TechSwordOverloadBeam.startBeam(serverLevel, player, start, end, visualTicks, chargePercent);
    }

    private List<LivingEntity> findBeamTargets(ServerLevel serverLevel, Player player, Vec3 start, Vec3 end, int maxTargets) {
        Vec3 delta = end.subtract(start);
        AABB searchBox = new AABB(start, end).inflate(0.75D);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity.isAlive() && entity != player);
        List<BeamTargetHit> matches = new java.util.ArrayList<>();

        for (LivingEntity candidate : nearby) {
            AABB targetBox = candidate.getBoundingBox().inflate(0.35D);
            Vec3 hitPoint = targetBox.clip(start, end).orElseGet(() -> {
                Vec3 center = targetBox.getCenter();
                Vec3 closestPoint = closestPointOnSegment(start, end, center);
                double distance = center.distanceTo(closestPoint);
                return distance <= 0.75D ? closestPoint : null;
            });

            if (hitPoint == null) {
                continue;
            }

            matches.add(new BeamTargetHit(candidate, start.distanceToSqr(hitPoint)));
        }

        matches.sort((left, right) -> Double.compare(left.distanceSqr(), right.distanceSqr()));
        if (matches.size() > maxTargets) {
            matches = matches.subList(0, maxTargets);
        }
        return matches.stream().map(BeamTargetHit::entity).toList();
    }

    private static Vec3 closestPointOnSegment(Vec3 start, Vec3 end, Vec3 point) {
        Vec3 delta = end.subtract(start);
        double lengthSqr = delta.lengthSqr();
        if (lengthSqr <= 0.0001D) {
            return start;
        }
        double t = Mth.clamp(point.subtract(start).dot(delta) / lengthSqr, 0.0D, 1.0D);
        return start.add(delta.scale(t));
    }

    private record BeamTargetHit(LivingEntity entity, double distanceSqr) {
    }

    private Component getAbilityDisplayName(String abilityId) {
        return switch (abilityId) {
            case "overload" -> Component.translatable("ability.microtech.overload");
            default -> Component.literal(prettifyAbilityId(abilityId));
        };
    }

    private static String prettifyAbilityId(String abilityId) {
        String[] parts = abilityId.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() == 0 ? abilityId : builder.toString();
    }
}
