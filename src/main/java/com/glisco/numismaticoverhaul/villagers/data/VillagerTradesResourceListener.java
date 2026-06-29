package com.glisco.numismaticoverhaul.villagers.data;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Villager trades are now data-driven in MC 26.2.
 * The TRADES map has been removed. This listener is stubbed until
 * a new data-driven approach is implemented.
 */
public class VillagerTradesResourceListener implements PreparableReloadListener {
    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor prepareExecutor, PreparationBarrier barrier, Executor applyExecutor) {
        NumismaticOverhaul.LOGGER.warn("[Numismatic Overhaul] Villager trades are data-driven in MC 26.2. Custom trade registration not yet implemented.");
        return CompletableFuture.completedFuture(null);
    }
}
