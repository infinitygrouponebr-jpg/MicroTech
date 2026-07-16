package Infinitygroup.microtech.energy;

import Infinitygroup.microtech.Microtech;
import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import Infinitygroup.microtech.energy.EnergyEndpointInfo;
import Infinitygroup.microtech.energy.EnergyNetworkInfo;
import org.slf4j.Logger;

public final class EnergyNetworkHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_ENERGY_NETWORK = false;
    public static final int MAX_NETWORK_CABLES = 256;
    public static final int CABLE_T1_TRANSFER_PER_CABLE = 20;
    public static final int CABLE_T1_MAX_NETWORK_TRANSFER = 200;

    private EnergyNetworkHelper() {
    }

    public static boolean tickCableNetwork(Level level, BlockPos startPos) {
        if (level.isClientSide) {
            return false;
        }

        Network network = collectNetwork(level, startPos);
        if (network.tooLarge) {
            if (DEBUG_ENERGY_NETWORK) {
                LOGGER.debug("[MicroTech EnergyNet] network overflow at {}", startPos);
            }
            return false;
        }

        BlockPos controller = findController(network.cables);

        if (!Objects.equals(startPos, controller)) {
            return false;
        }

        List<Endpoint> sources = collectSources(level, network);
        List<Endpoint> destinations = collectDestinations(level, network);
        if (sources.isEmpty() || destinations.isEmpty()) {
            return false;
        }

        int networkLimit = Math.min(CABLE_T1_MAX_NETWORK_TRANSFER, network.cables.size() * CABLE_T1_TRANSFER_PER_CABLE);
        if (networkLimit <= 0) {
            return false;
        }

        int totalAvailable = 0;
        for (Endpoint source : sources) {
            totalAvailable += Math.max(0, source.storage.extractEnergy(networkLimit, true));
        }

        int totalDemand = 0;
        for (Endpoint destination : destinations) {
            totalDemand += Math.max(0, destination.storage.receiveEnergy(networkLimit, true));
        }

        int transferBudget = Math.min(networkLimit, Math.min(totalAvailable, totalDemand));
        if (transferBudget <= 0) {
            return false;
        }

        long seed = level.getGameTime() ^ controller.asLong();
        int sourceStart = Math.floorMod((int) seed, sources.size());
        int destinationStart = Math.floorMod((int) (seed >>> 1), destinations.size());

        int transferred = distributeEnergy(level, sources, destinations, sourceStart, destinationStart, transferBudget);
        if (transferred > 0) {
            if (DEBUG_ENERGY_NETWORK) {
                LOGGER.debug("[MicroTech EnergyNet] cables={} sources={} targets={} available={} demand={} transferred={}",
                        network.cables.size(), sources.size(), destinations.size(), totalAvailable, totalDemand, transferred);
            }
            return true;
        }

        return false;
    }

    public static EnergyNetworkInfo analyzeNetwork(Level level, BlockPos cablePos) {
        if (level == null || cablePos == null) {
            return new EnergyNetworkInfo(0, MAX_NETWORK_CABLES, 0, 0, 0, 0, 0, 0, BlockPos.ZERO, false, List.of(), List.of());
        }

        Network network = collectNetwork(level, cablePos);
        BlockPos controller = network.cables.isEmpty() ? cablePos : findController(network.cables);
        EndpointCollection endpoints = collectEndpoints(level, network);

        int totalAvailable = 0;
        for (EndpointData endpoint : endpoints.entries().values()) {
            totalAvailable += Math.max(0, endpoint.available);
        }

        int totalDemand = 0;
        for (EndpointData endpoint : endpoints.entries().values()) {
            totalDemand += Math.max(0, endpoint.demand);
        }

        int networkLimit = Math.min(CABLE_T1_MAX_NETWORK_TRANSFER, network.cables.size() * CABLE_T1_TRANSFER_PER_CABLE);
        int estimatedTransfer = Math.min(networkLimit, Math.min(totalAvailable, totalDemand));

        List<EnergyEndpointInfo> sources = new ArrayList<>();
        List<EnergyEndpointInfo> targets = new ArrayList<>();
        for (EndpointData endpoint : endpoints.entries().values()) {
            EnergyEndpointInfo info = endpoint.toInfo();
            if (endpoint.canExtract && endpoint.available > 0) {
                sources.add(info);
            }
            if (endpoint.canReceive && endpoint.demand > 0) {
                targets.add(info);
            }
        }

        sources.sort(Comparator.comparingLong((EnergyEndpointInfo endpoint) -> endpoint.pos().asLong())
                .thenComparingInt(endpoint -> endpoint.side().ordinal()));
        targets.sort(Comparator.comparingLong((EnergyEndpointInfo endpoint) -> endpoint.pos().asLong())
                .thenComparingInt(endpoint -> endpoint.side().ordinal()));

        return new EnergyNetworkInfo(
                network.cables.size(),
                MAX_NETWORK_CABLES,
                sources.size(),
                targets.size(),
                totalAvailable,
                totalDemand,
                networkLimit,
                estimatedTransfer,
                controller,
                network.tooLarge,
                List.copyOf(sources),
                List.copyOf(targets)
        );
    }

    private static BlockPos findController(List<BlockPos> cables) {
        if (cables.isEmpty()) {
            return BlockPos.ZERO;
        }

        BlockPos controller = cables.get(0);
        for (int i = 1; i < cables.size(); i++) {
            BlockPos candidate = cables.get(i);
            if (candidate.asLong() < controller.asLong()) {
                controller = candidate;
            }
        }
        return controller;
    }

    private static EndpointCollection collectEndpoints(Level level, Network network) {
        Map<EndpointKey, EndpointData> unique = new HashMap<>();

        for (BlockPos cablePos : network.cables) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(direction);
                if (network.visited.contains(neighborPos)) {
                    continue;
                }

                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
                if (storage == null) {
                    continue;
                }

                EndpointKey key = new EndpointKey(neighborPos, direction.getOpposite());
                EndpointData data = unique.computeIfAbsent(key, unused -> new EndpointData(neighborPos, direction.getOpposite()));
                data.canExtract |= storage.canExtract();
                data.canReceive |= storage.canReceive();
                if (data.canExtract) {
                    data.available = Math.max(data.available, Math.max(0, storage.extractEnergy(CABLE_T1_TRANSFER_PER_CABLE, true)));
                }
                if (data.canReceive) {
                    data.demand = Math.max(data.demand, Math.max(0, storage.receiveEnergy(CABLE_T1_TRANSFER_PER_CABLE, true)));
                }
            }
        }

        return new EndpointCollection(unique);
    }

    private static int distributeEnergy(Level level, List<Endpoint> sources, List<Endpoint> destinations, int sourceStart, int destinationStart, int transferBudget) {
        int remainingBudget = transferBudget;
        int transferred = 0;

        List<Endpoint> rotatedDestinations = rotate(destinations, destinationStart);
        for (int destinationIndex = 0; destinationIndex < rotatedDestinations.size() && remainingBudget > 0; destinationIndex++) {
            Endpoint destination = rotatedDestinations.get(destinationIndex);
            int destinationsLeft = rotatedDestinations.size() - destinationIndex;
            int destinationBudget = Math.max(1, remainingBudget / destinationsLeft);
            int destinationRemaining = destinationBudget;

            List<Endpoint> rotatedSources = rotate(sources, Math.floorMod(sourceStart + destinationIndex, sources.size()));
            for (Endpoint source : rotatedSources) {
                if (remainingBudget <= 0 || destinationRemaining <= 0) {
                    break;
                }

                if (source.position.equals(destination.position)) {
                    continue;
                }

                int probe = Math.min(remainingBudget, destinationRemaining);
                int sourceCanExtract = source.storage.extractEnergy(probe, true);
                if (sourceCanExtract <= 0) {
                    continue;
                }

                int destinationCanReceive = destination.storage.receiveEnergy(sourceCanExtract, true);
                int transferable = Math.min(remainingBudget, Math.min(destinationRemaining, Math.min(sourceCanExtract, destinationCanReceive)));
                if (transferable <= 0) {
                    continue;
                }

                int extracted = source.storage.extractEnergy(transferable, false);
                if (extracted <= 0) {
                    continue;
                }

                int inserted = destination.storage.receiveEnergy(extracted, false);
                if (inserted < extracted) {
                    int refund = extracted - inserted;
                    if (refund > 0) {
                        source.storage.receiveEnergy(refund, false);
                    }
                }

                if (inserted > 0) {
                    remainingBudget -= inserted;
                    destinationRemaining -= inserted;
                    transferred += inserted;
                }
            }
        }

        return transferred;
    }

    private static List<Endpoint> collectSources(Level level, Network network) {
        Map<EndpointKey, Endpoint> unique = new HashMap<>();

        for (BlockPos cablePos : network.cables) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(direction);
                if (network.visited.contains(neighborPos)) {
                    continue;
                }

                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
                if (storage == null || !storage.canExtract()) {
                    continue;
                }

                if (storage.extractEnergy(CABLE_T1_TRANSFER_PER_CABLE, true) <= 0) {
                    continue;
                }

                EndpointKey key = new EndpointKey(neighborPos, direction.getOpposite());
                unique.putIfAbsent(key, new Endpoint(neighborPos, direction.getOpposite(), storage));
            }
        }

        return sortEndpoints(unique.values());
    }

    private static List<Endpoint> collectDestinations(Level level, Network network) {
        Map<EndpointKey, Endpoint> unique = new HashMap<>();

        for (BlockPos cablePos : network.cables) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(direction);
                if (network.visited.contains(neighborPos)) {
                    continue;
                }

                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
                if (storage == null || !storage.canReceive()) {
                    continue;
                }

                if (storage.receiveEnergy(CABLE_T1_TRANSFER_PER_CABLE, true) <= 0) {
                    continue;
                }

                EndpointKey key = new EndpointKey(neighborPos, direction.getOpposite());
                unique.putIfAbsent(key, new Endpoint(neighborPos, direction.getOpposite(), storage));
            }
        }

        return sortEndpoints(unique.values());
    }

    private static List<Endpoint> sortEndpoints(Iterable<Endpoint> endpoints) {
        List<Endpoint> ordered = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            ordered.add(endpoint);
        }

        ordered.sort(Comparator.comparingLong((Endpoint endpoint) -> endpoint.position.asLong())
                .thenComparingInt(endpoint -> endpoint.side.ordinal()));
        return ordered;
    }

    private static List<Endpoint> rotate(List<Endpoint> endpoints, int startIndex) {
        if (endpoints.isEmpty()) {
            return endpoints;
        }

        int normalized = Math.floorMod(startIndex, endpoints.size());
        if (normalized == 0) {
            return endpoints;
        }

        List<Endpoint> rotated = new ArrayList<>(endpoints.size());
        rotated.addAll(endpoints.subList(normalized, endpoints.size()));
        rotated.addAll(endpoints.subList(0, normalized));
        return rotated;
    }

    private static Network collectNetwork(Level level, BlockPos startPos) {
        if (!level.getBlockState(startPos).is(Microtech.CABLE_T1.get())) {
            return new Network(List.of(), Set.of(), true);
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> cables = new ArrayList<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            cables.add(current);
            if (cables.size() > MAX_NETWORK_CABLES) {
                return new Network(cables, visited, true);
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = current.relative(direction);
                if (visited.contains(neighborPos)) {
                    continue;
                }

                if (!level.getBlockState(neighborPos).is(Microtech.CABLE_T1.get())) {
                    continue;
                }

                visited.add(neighborPos);
                queue.addLast(neighborPos);
            }
        }

        cables.sort(Comparator.comparingLong(BlockPos::asLong));
        return new Network(cables, visited, false);
    }

    private record EndpointCollection(Map<EndpointKey, EndpointData> entries) {
    }

    private record Network(List<BlockPos> cables, Set<BlockPos> visited, boolean tooLarge) {
    }

    private record Endpoint(BlockPos position, Direction side, IEnergyStorage storage) {
    }

    private record EndpointKey(BlockPos position, Direction side) {
    }

    private static final class EndpointData {
        private final BlockPos position;
        private final Direction side;
        private int available;
        private int demand;
        private boolean canExtract;
        private boolean canReceive;

        private EndpointData(BlockPos position, Direction side) {
            this.position = position;
            this.side = side;
        }

        private EnergyEndpointInfo toInfo() {
            return new EnergyEndpointInfo(this.position, this.side, this.available, this.demand, this.canExtract, this.canReceive);
        }
    }
}
