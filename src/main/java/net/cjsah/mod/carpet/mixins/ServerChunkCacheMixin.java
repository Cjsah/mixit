package net.cjsah.mod.carpet.mixins;

import net.cjsah.mod.carpet.fakes.ServerChunkManagerInterface;
import net.cjsah.mod.carpet.utils.SpawnReporter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin implements ServerChunkManagerInterface {
    @Shadow @Final private ServerLevel level;

    @Shadow @Final private DistanceManager distanceManager;

    @Override // shared between scarpet and spawnChunks setting
    public DistanceManager getCMTicketManager() {
        return distanceManager;
    }

    @Redirect(method = "tickChunks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/DistanceManager;getNaturalSpawnChunkCount()I"
    ))
    //this runs once per world spawning cycle. Allows to grab mob counts and count spawn ticks
    private int setupTracking(DistanceManager chunkTicketManager) {
        int j = chunkTicketManager.getNaturalSpawnChunkCount();
        ResourceKey<Level> dim = this.level.dimension(); // getDimensionType;
        //((WorldInterface)world).getPrecookedMobs().clear(); not needed because mobs are compared with predefined BBs
        SpawnReporter.chunkCounts.put(dim, j);

        if (SpawnReporter.track_spawns > 0L) {
            //local spawns now need to be tracked globally cause each calll is just for chunk
            SpawnReporter.local_spawns = new HashMap<>();
            SpawnReporter.first_chunk_marker = new HashSet<>();
            for (MobCategory cat : MobCategory.values()) {
                Pair<ResourceKey<Level>, MobCategory> key = Pair.of(dim, cat);
                SpawnReporter.overall_spawn_ticks.put(key,
                        SpawnReporter.overall_spawn_ticks.get(key)+
                        SpawnReporter.spawn_tries.get(cat));
            }
        }
        return j;
    }


    @Inject(method = "tickChunks", at = @At("RETURN"))
    private void onFinishSpawnWorldCycle(CallbackInfo ci) {
        LevelData levelProperties_1 = this.level.getLevelData(); // levelProperies class
        boolean boolean_3 = levelProperties_1.getGameTime() % 400L == 0L;
        if (SpawnReporter.track_spawns > 0L && SpawnReporter.local_spawns != null) {
            for (MobCategory cat: MobCategory.values()) {
                ResourceKey<Level> dim = level.dimension(); // getDimensionType;
                Pair<ResourceKey<Level>, MobCategory> key = Pair.of(dim, cat);
                int spawnTries = SpawnReporter.spawn_tries.get(cat);
                if (!SpawnReporter.local_spawns.containsKey(cat)) {
                    if (!cat.isPersistent() || boolean_3) { // isAnimal
                        // fill mobcaps for that category so spawn got cancelled
                        SpawnReporter.spawn_ticks_full.put(key,
                                SpawnReporter.spawn_ticks_full.get(key)+ spawnTries);
                    }

                }
                else if (SpawnReporter.local_spawns.get(cat) > 0) {
                    // tick spawned mobs for that type
                    SpawnReporter.spawn_ticks_succ.put(key,
                        SpawnReporter.spawn_ticks_succ.get(key)+spawnTries);
                    SpawnReporter.spawn_ticks_spawns.put(key,
                        SpawnReporter.spawn_ticks_spawns.get(key)+
                        SpawnReporter.local_spawns.get(cat));
                        // this will be off comparing to 1.13 as that would succeed if
                        // ANY tries in that round were successful.
                        // there will be much more difficult to mix in
                        // considering spawn tries to remove, as with warp
                        // there is little need for them anyways.
                }
                else { // spawn no mobs despite trying
                    //tick didn's spawn mobs of that type
                    SpawnReporter.spawn_ticks_fail.put(key,
                        SpawnReporter.spawn_ticks_fail.get(key)+spawnTries);
                }
            }
        }
        SpawnReporter.local_spawns = null;
    }



}
