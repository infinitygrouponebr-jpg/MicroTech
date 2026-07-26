package Infinitygroup.microtech.entity.control;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;
import java.util.UUID;

public final class ControlledMobDebugCommand {
    private ControlledMobDebugCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("microtech")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("controller")
                        .then(Commands.literal("debug")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(ControlledMobDebugCommand::debugEntity))
                                .then(Commands.argument("uuid", StringArgumentType.word())
                                        .executes(ControlledMobDebugCommand::debugUuid)))));
    }

    private static int debugEntity(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "entity");
        sendDebug(context.getSource(), entity);
        return 1;
    }

    private static int debugUuid(CommandContext<CommandSourceStack> context) {
        try {
            UUID uuid = UUID.fromString(StringArgumentType.getString(context, "uuid"));
            Entity entity = context.getSource().getLevel().getEntity(uuid);
            if (entity == null) {
                context.getSource().sendFailure(Component.literal("Entidade nao carregada: " + uuid));
                return 0;
            }
            sendDebug(context.getSource(), entity);
            return 1;
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFailure(Component.literal("UUID invalido."));
            return 0;
        }
    }

    private static void sendDebug(CommandSourceStack source, Entity entity) {
        source.sendSuccess(() -> Component.literal("MicroTech Controller Debug: " + describe(entity)), false);
        source.sendSuccess(() -> Component.literal("controlador=" + ControlledMobAllianceService.resolveController(entity).map(UUID::toString).orElse("nenhum")
                + " controlado=" + ControlledMobData.isControlled(entity)
                + " tier=" + (ControlledMobData.isControlled(entity) ? ControlledMobData.getTier(entity).name() : "none")), false);

        if (entity instanceof Warden warden) {
            sendWardenDebug(source, warden);
        } else if (entity instanceof WitherBoss wither) {
            sendWitherDebug(source, wither);
        } else if (entity instanceof Mob mob) {
            LivingEntity target = mob.getTarget();
            source.sendSuccess(() -> Component.literal("alvo=" + describe(target)
                    + " alvoValido=" + (target != null && ControlledMobAllianceService.canControlledMobTarget(mob, target))), false);
        }
    }

    private static void sendWardenDebug(CommandSourceStack source, Warden warden) {
        LivingEntity brainTarget = memoryEntity(warden, MemoryModuleType.ATTACK_TARGET).orElse(null);
        LivingEntity roarTarget = memoryEntity(warden, MemoryModuleType.ROAR_TARGET).orElse(null);
        LivingEntity nearestAttackable = memoryEntity(warden, MemoryModuleType.NEAREST_ATTACKABLE).orElse(null);
        Object disturbance = warden.getBrain().getMemory(MemoryModuleType.DISTURBANCE_LOCATION).map(Object::toString).orElse("nenhuma");
        boolean sonicCooldown = warden.getBrain().getMemory(MemoryModuleType.SONIC_BOOM_COOLDOWN).isPresent();
        String commanded = ControlledMobAllianceService.resolveController(warden)
                .flatMap(ControlledMobCommandTargetService::getCommand)
                .map(command -> command.target() + " expiraEm=" + (command.expiresAt() - warden.level().getGameTime()))
                .orElse("nenhum");
        String pulse = ControlledWardenDarknessTracker.getLastPulse(warden.getUUID())
                .map(last -> "tick=" + last.gameTime() + " raio=" + last.radius())
                .orElse("nenhuma");

        source.sendSuccess(() -> Component.literal("warden brainTarget=" + describe(brainTarget)
                + " roarTarget=" + describe(roarTarget)
                + " nearestAttackable=" + describe(nearestAttackable)), false);
        source.sendSuccess(() -> Component.literal("warden raivaAtiva=" + warden.getAngerManagement().getActiveAnger(brainTarget)
                + " perturbadora=" + disturbance
                + " alvoComandado=" + commanded
                + " sonicCooldown=" + sonicCooldown
                + " ultimaDarkness=" + pulse), false);
    }

    private static void sendWitherDebug(CommandSourceStack source, WitherBoss wither) {
        String commanded = ControlledMobAllianceService.resolveController(wither)
                .flatMap(ControlledMobCommandTargetService::getCommand)
                .map(command -> command.target() + " expiraEm=" + (command.expiresAt() - wither.level().getGameTime()))
                .orElse("nenhum");
        Optional<UUID> lastProjectile = ControlledTemporaryEntityTracker.getLastProjectile(wither.getUUID());

        source.sendSuccess(() -> Component.literal("wither alvoPrincipal=" + describe(wither.getTarget())
                + " alvoComandado=" + commanded
                + " ultimoProjetil=" + lastProjectile.map(UUID::toString).orElse("nenhum")), false);
        for (int head = 0; head < 3; head++) {
            int targetId = wither.getAlternativeTarget(head);
            Entity target = resolveById(wither, targetId);
            boolean valid = target instanceof LivingEntity living && ControlledMobAllianceService.canControlledMobTarget(wither, living);
            int headIndex = head;
            source.sendSuccess(() -> Component.literal("cabeca" + headIndex + " id=" + targetId
                    + " alvo=" + describe(target)
                    + " valido=" + valid), false);
        }
        source.sendSuccess(() -> Component.literal("friendlyFire="
                + (Infinitygroup.microtech.Config.controlledWitherFriendlyFire || Infinitygroup.microtech.Config.controlledWitherDamagesAllies)
                + " blockGriefing=" + Infinitygroup.microtech.Config.controlledWitherBlockGriefing), false);
    }

    private static Optional<? extends LivingEntity> memoryEntity(Warden warden, MemoryModuleType<? extends LivingEntity> memory) {
        return warden.getBrain().getMemory(memory);
    }

    private static Entity resolveById(Entity source, int id) {
        if (id <= 0 || !(source.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(id);
    }

    private static String describe(Entity entity) {
        if (entity == null) {
            return "nenhum";
        }
        return entity.getType().toShortString() + "#" + entity.getId() + "/" + entity.getUUID();
    }
}
