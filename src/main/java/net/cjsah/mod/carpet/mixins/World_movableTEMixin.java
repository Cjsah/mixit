package net.cjsah.mod.carpet.mixins;

import net.cjsah.mod.carpet.fakes.WorldChunkInterface;
import net.cjsah.mod.carpet.fakes.WorldInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Level.class)
public abstract class World_movableTEMixin implements WorldInterface, LevelAccessor
{
    @Shadow
    @Final
    public boolean isClient;

    @Shadow
    public abstract LevelChunk getWorldChunk(BlockPos blockPos_1);
    
    @Shadow
    public abstract BlockState getBlockState(BlockPos blockPos_1);
    
    //@Shadow
    //public abstract ChunkManager getChunkManager();
    
    @Shadow
    public abstract void scheduleBlockRerenderIfNeeded(BlockPos blockPos_1, BlockState s1, BlockState s2);
    
    @Shadow
    public abstract void updateListeners(BlockPos var1, BlockState var2, BlockState var3, int var4);
    
    @Shadow
    public abstract void updateNeighborsAlways(BlockPos blockPos_1, Block block_1);
    
    @Shadow
    public abstract void onBlockChanged(BlockPos blockPos_1, BlockState blockState_1, BlockState blockState_2);

    @Shadow public abstract ProfilerFiller getProfiler();

    @Shadow public abstract void updateComparators(BlockPos pos, Block block);

    //@Shadow public abstract boolean setBlockState(BlockPos pos, BlockState state, int flags);

    @Shadow public abstract boolean isDebugWorld();

    /**
     * @author 2No2Name
     */
    public boolean setBlockStateWithBlockEntity(BlockPos blockPos_1, BlockState blockState_1, BlockEntity newBlockEntity, int int_1)
    {
        if (isOutsideBuildHeight(blockPos_1) || !this.isClient && isDebugWorld()) return false;
        LevelChunk worldChunk_1 = this.getWorldChunk(blockPos_1);
        Block block_1 = blockState_1.getBlock();

        BlockState blockState_2;
        if (newBlockEntity != null && block_1 instanceof EntityBlock)
            blockState_2 = ((WorldChunkInterface) worldChunk_1).setBlockStateWithBlockEntity(blockPos_1, blockState_1, newBlockEntity, (int_1 & 64) != 0);
        else
            blockState_2 = worldChunk_1.setBlockState(blockPos_1, blockState_1, (int_1 & 64) != 0);

        if (blockState_2 == null)
        {
            return false;
        }
        else
        {
            BlockState blockState_3 = this.getBlockState(blockPos_1);

            if (blockState_3 != blockState_2 && (blockState_3.getLightBlock((BlockGetter) this, blockPos_1) != blockState_2.getLightBlock((BlockGetter) this, blockPos_1) || blockState_3.getLightEmission() != blockState_2.getLightEmission() || blockState_3.useShapeForLightOcclusion() || blockState_2.useShapeForLightOcclusion()))
            {
                ProfilerFiller profiler = getProfiler();
                profiler.push("queueCheckLight");
                this.getChunkSource().getLightEngine().checkBlock(blockPos_1);
                profiler.pop();
            }

            if (blockState_3 == blockState_1)
            {
                if (blockState_2 != blockState_3)
                {
                    this.scheduleBlockRerenderIfNeeded(blockPos_1, blockState_2, blockState_3);
                }

                if ((int_1 & 2) != 0 && (!this.isClient || (int_1 & 4) == 0) && (this.isClient || worldChunk_1.getFullStatus() != null && worldChunk_1.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING)))
                {
                    this.updateListeners(blockPos_1, blockState_2, blockState_1, int_1);
                }

                if (!this.isClient && (int_1 & 1) != 0)
                {
                    this.updateNeighborsAlways(blockPos_1, blockState_2.getBlock());
                    if (blockState_1.hasAnalogOutputSignal())
                    {
                        updateComparators(blockPos_1, block_1);
                    }
                }

                if ((int_1 & 16) == 0)
                {
                    int int_2 = int_1 & -34;
                    blockState_2.updateIndirectNeighbourShapes(this, blockPos_1, int_2); // prepare
                    blockState_1.updateNeighbourShapes(this, blockPos_1, int_2); // updateNeighbours
                    blockState_1.updateIndirectNeighbourShapes(this, blockPos_1, int_2); // prepare
                }
                this.onBlockChanged(blockPos_1, blockState_2, blockState_3);
            }
            return true;
        }
    }
}