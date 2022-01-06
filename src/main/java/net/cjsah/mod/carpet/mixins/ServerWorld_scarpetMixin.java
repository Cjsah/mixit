package net.cjsah.mod.carpet.mixins;

import net.cjsah.mod.carpet.fakes.ServerWorldInterface;
import net.cjsah.mod.carpet.script.CarpetEventServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerLevel.class)
public class ServerWorld_scarpetMixin implements ServerWorldInterface
{
    @Inject(method = "tickChunk", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z",
            shift = At.Shift.BEFORE,
            ordinal = 1
    ))
    private void onNaturalLightinig(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci,
                                    //ChunkPos chunkPos, boolean bl, int i, int j, Profiler profiler, BlockPos blockPos, boolean bl2)
                                    ChunkPos chunkPos, boolean bl, int i, int j, ProfilerFiller profiler, BlockPos blockPos, boolean bl2, LightningBolt lightningEntity)
    {
        if (CarpetEventServer.Event.LIGHTNING.isNeeded()) CarpetEventServer.Event.LIGHTNING.onWorldEventFlag((ServerLevel) (Object)this, blockPos, bl2?1:0);
    }

    /*
    moved to ServerEntityManager_scarpetMixin in 1.17
    @Redirect(method = "addEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerEntityManager;addEntity(Lnet/minecraft/world/entity/EntityLike;)Z"
    ))
    private boolean onEntityAddedToWorld(ServerEntityManager serverEntityManager, EntityLike entityLike)
    {
        Entity entity = (Entity)entityLike;
        boolean success = serverEntityManager.addEntity(entity);
        if (success) {
            CarpetEventServer.Event event = ENTITY_LOAD.get(entity.getType());
            if (event != null) {
                if (event.isNeeded()) {
                    event.onEntityAction(entity);
                }
            } else {
                CarpetSettings.LOG.error("Failed to handle entity " + entity.getType().getTranslationKey());
            }
        };
        return success;
    }
    */

    @Inject(method = "createExplosion", at = @At("HEAD"))
    private void handleExplosion(/*@Nullable*/ Entity entity, /*@Nullable*/ DamageSource damageSource, /*@Nullable*/ ExplosionDamageCalculator explosionBehavior, double d, double e, double f, float g, boolean bl, Explosion.BlockInteraction destructionType, CallbackInfoReturnable<Explosion> cir)
    {
        if (CarpetEventServer.Event.EXPLOSION.isNeeded())
            CarpetEventServer.Event.EXPLOSION.onExplosion((ServerLevel) (Object)this, entity, null, d, e, f, g, bl, null, null, destructionType);
    }

    @Final
    @Shadow
    private ServerLevelData worldProperties;
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;

    public ServerLevelData getWorldPropertiesCM(){
        return worldProperties;
    }

    @Override
    public LevelEntityGetter<Entity> getEntityLookupCMPublic() {
        return entityManager.getEntityGetter();
    }
}