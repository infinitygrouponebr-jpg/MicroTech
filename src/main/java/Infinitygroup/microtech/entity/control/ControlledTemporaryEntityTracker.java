package Infinitygroup.microtech.entity.control;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ControlledTemporaryEntityTracker {
    private static final long DEFAULT_LIFETIME = 20L * 60L;
    private static final Map<UUID, TemporaryControl> TEMPORARY_CONTROLS = new HashMap<>();
    private static final Map<UUID, UUID> LAST_PROJECTILE_BY_CREATOR = new HashMap<>();

    private ControlledTemporaryEntityTracker() {
    }

    public static void registerIfControlled(Entity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        Entity creator = resolveCreator(entity).orElse(null);
        if (creator == null) {
            return;
        }

        ControlledMobData.getController(creator).ifPresent(controller -> register(entity, creator, controller));
    }

    public static void register(Entity entity, Entity creator, UUID controller) {
        if (entity.level().isClientSide()) {
            return;
        }

        long gameTime = entity.level().getGameTime();
        ResourceLocation creatorType = BuiltInRegistries.ENTITY_TYPE.getKey(creator.getType());
        TEMPORARY_CONTROLS.put(entity.getUUID(), new TemporaryControl(
                controller,
                creator.getUUID(),
                creatorType,
                gameTime,
                gameTime + DEFAULT_LIFETIME
        ));

        if (entity instanceof Projectile || entity instanceof WitherSkull) {
            LAST_PROJECTILE_BY_CREATOR.put(creator.getUUID(), entity.getUUID());
        }
    }

    public static Optional<UUID> getController(Entity entity) {
        return getRecord(entity).map(TemporaryControl::controller);
    }

    public static Optional<UUID> getCreator(Entity entity) {
        return getRecord(entity).map(TemporaryControl::creator);
    }

    public static Optional<UUID> getLastProjectile(UUID creator) {
        return Optional.ofNullable(LAST_PROJECTILE_BY_CREATOR.get(creator));
    }

    public static boolean isCreatedByControlledWither(Entity entity) {
        Entity directCreator = resolveCreator(entity).orElse(null);
        if (directCreator != null && ControlledMobData.isControlled(directCreator)
                && BuiltInRegistries.ENTITY_TYPE.getKey(directCreator.getType()).getPath().equals("wither")) {
            return true;
        }

        return getRecord(entity)
                .map(TemporaryControl::creatorType)
                .map(type -> "wither".equals(type.getPath()))
                .orElse(false);
    }

    public static Optional<Entity> resolveCreator(Entity entity) {
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner != null) {
                return Optional.of(owner);
            }
        }

        if (entity instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner != null) {
                return Optional.of(owner);
            }
        }

        return resolveReflectiveOwner(entity);
    }

    public static Optional<Entity> resolveHitEntity(HitResult result) {
        if (result instanceof EntityHitResult entityHitResult) {
            return Optional.of(entityHitResult.getEntity());
        }
        return Optional.empty();
    }

    public static void unregister(Entity entity) {
        TEMPORARY_CONTROLS.remove(entity.getUUID());
        LAST_PROJECTILE_BY_CREATOR.entrySet().removeIf(entry -> entry.getValue().equals(entity.getUUID()));
    }

    public static void cleanup(Level level) {
        if (level.isClientSide()) {
            return;
        }

        long gameTime = level.getGameTime();
        Iterator<Map.Entry<UUID, TemporaryControl>> iterator = TEMPORARY_CONTROLS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TemporaryControl> entry = iterator.next();
            if (entry.getValue().expiresAt() <= gameTime) {
                iterator.remove();
            }
        }
    }

    private static Optional<TemporaryControl> getRecord(Entity entity) {
        TemporaryControl record = TEMPORARY_CONTROLS.get(entity.getUUID());
        if (record == null) {
            return Optional.empty();
        }

        if (entity.level() instanceof ServerLevel serverLevel && record.expiresAt() <= serverLevel.getGameTime()) {
            unregister(entity);
            return Optional.empty();
        }

        return Optional.of(record);
    }

    private static Optional<Entity> resolveReflectiveOwner(Entity entity) {
        for (String methodName : new String[]{"getOwner", "getOwnerEntity"}) {
            try {
                Method method = entity.getClass().getMethod(methodName);
                if (!Entity.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }

                Object owner = method.invoke(entity);
                if (owner instanceof Entity ownerEntity) {
                    return Optional.of(ownerEntity);
                }
            } catch (ReflectiveOperationException ignored) {
                // Some vanilla and modded temporary entities expose their owner through different optional APIs.
            }
        }

        return Optional.empty();
    }

    private record TemporaryControl(UUID controller, UUID creator, ResourceLocation creatorType, long createdAt, long expiresAt) {
    }
}
