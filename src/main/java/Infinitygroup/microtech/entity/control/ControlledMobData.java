package Infinitygroup.microtech.entity.control;

import Infinitygroup.microtech.Microtech;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public final class ControlledMobData {
    public static final int DATA_VERSION = 1;

    private static final String ROOT = Microtech.MODID + "_controller_chip";
    private static final String KEY_CONTROLLED = "microtech_controlled";
    private static final String KEY_CONTROLLER = "controller";
    private static final String KEY_ORDER = "order";
    private static final String KEY_GUARD_POS = "guard_pos";
    private static final String KEY_GUARD_DIMENSION = "guard_dimension";
    private static final String KEY_VERSION = "version";
    private static final String KEY_TELEPORT_COOLDOWN = "teleport_cooldown";

    private ControlledMobData() {
    }

    public static boolean isControlled(Entity entity) {
        CompoundTag root = readRoot(entity);
        return root != null && root.getBoolean(KEY_CONTROLLED) && root.hasUUID(KEY_CONTROLLER);
    }

    public static Optional<UUID> getController(Entity entity) {
        CompoundTag root = readRoot(entity);
        if (root == null || !root.getBoolean(KEY_CONTROLLED) || !root.hasUUID(KEY_CONTROLLER)) {
            return Optional.empty();
        }
        return Optional.of(root.getUUID(KEY_CONTROLLER));
    }

    public static boolean isControlledBy(Entity entity, UUID playerId) {
        return getController(entity).map(playerId::equals).orElse(false);
    }

    public static ControlledMobOrder getOrder(Entity entity) {
        CompoundTag root = readRoot(entity);
        return root == null ? ControlledMobOrder.FOLLOW : ControlledMobOrder.byId(root.getString(KEY_ORDER));
    }

    public static void setOrder(Entity entity, ControlledMobOrder order) {
        getRoot(entity).putString(KEY_ORDER, order.getId());
    }

    public static void install(Entity entity, UUID controllerId) {
        CompoundTag root = getRoot(entity);
        root.putBoolean(KEY_CONTROLLED, true);
        root.putUUID(KEY_CONTROLLER, controllerId);
        root.putString(KEY_ORDER, ControlledMobOrder.FOLLOW.getId());
        root.putInt(KEY_VERSION, DATA_VERSION);
        root.putInt(KEY_TELEPORT_COOLDOWN, 0);
        root.remove(KEY_GUARD_POS);
        root.remove(KEY_GUARD_DIMENSION);
    }

    public static void remove(Entity entity) {
        entity.getPersistentData().remove(ROOT);
    }

    public static Optional<BlockPos> getGuardPos(Entity entity) {
        CompoundTag root = readRoot(entity);
        if (root == null) {
            return Optional.empty();
        }
        if (!root.contains(KEY_GUARD_POS, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag pos = root.getCompound(KEY_GUARD_POS);
        return Optional.of(new BlockPos(pos.getInt("x"), pos.getInt("y"), pos.getInt("z")));
    }

    public static Optional<ResourceKey<Level>> getGuardDimension(Entity entity) {
        CompoundTag root = readRoot(entity);
        if (root == null) {
            return Optional.empty();
        }
        String id = root.getString(KEY_GUARD_DIMENSION);
        if (id.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(id)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static void setGuard(Entity entity, BlockPos pos, ResourceKey<Level> dimension) {
        CompoundTag root = getRoot(entity);
        CompoundTag guardPos = new CompoundTag();
        guardPos.putInt("x", pos.getX());
        guardPos.putInt("y", pos.getY());
        guardPos.putInt("z", pos.getZ());
        root.put(KEY_GUARD_POS, guardPos);
        root.putString(KEY_GUARD_DIMENSION, dimension.location().toString());
        root.putString(KEY_ORDER, ControlledMobOrder.GUARD.getId());
    }

    public static int getTeleportCooldown(Entity entity) {
        CompoundTag root = readRoot(entity);
        return root == null ? 0 : Math.max(0, root.getInt(KEY_TELEPORT_COOLDOWN));
    }

    public static void setTeleportCooldown(Entity entity, int ticks) {
        getRoot(entity).putInt(KEY_TELEPORT_COOLDOWN, Math.max(0, ticks));
    }

    public static void tickTeleportCooldown(Entity entity) {
        int cooldown = getTeleportCooldown(entity);
        if (cooldown > 0) {
            setTeleportCooldown(entity, cooldown - 1);
        }
    }

    private static CompoundTag getRoot(Entity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }

    private static CompoundTag readRoot(Entity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
            return null;
        }
        return persistent.getCompound(ROOT);
    }
}
