package net.cjsah.mod.carpet.mixins;

import com.mojang.authlib.GameProfile;
import net.cjsah.mod.carpet.patches.EntityPlayerMPFake;
import net.cjsah.mod.carpet.patches.NetHandlerPlayServerFake;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerManager_fakePlayersMixin
{
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "loadPlayerData", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void fixStartingPos(ServerPlayer serverPlayerEntity_1, CallbackInfoReturnable<CompoundTag> cir)
    {
        if (serverPlayerEntity_1 instanceof EntityPlayerMPFake)
        {
            ((EntityPlayerMPFake) serverPlayerEntity_1).fixStartingPosition.run();
        }
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "NEW", target = "net/minecraft/server/network/ServerPlayNetworkHandler"))
    private ServerGamePacketListenerImpl replaceNetworkHandler(MinecraftServer server, Connection clientConnection, ServerPlayer playerIn)
    {
        boolean isServerPlayerEntity = playerIn instanceof EntityPlayerMPFake;
        if (isServerPlayerEntity)
        {
            return new NetHandlerPlayServerFake(this.server, clientConnection, playerIn);
        }
        else
        {
            return new ServerGamePacketListenerImpl(this.server, clientConnection, playerIn);
        }
    }

    @Redirect(method = "createPlayer", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z"))
    private boolean cancelWhileLoop(Iterator iterator)
    {
        return false;
    }

    @Inject(method = "createPlayer", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Ljava/util/Iterator;hasNext()Z"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void newWhileLoop(GameProfile gameProfile_1, CallbackInfoReturnable<ServerPlayer> cir, UUID uUID_1,
                              List list_1, Iterator var5)
    {
        while (var5.hasNext())
        {
            ServerPlayer serverPlayerEntity_3 = (ServerPlayer) var5.next();
            if(serverPlayerEntity_3 instanceof EntityPlayerMPFake)
            {
                ((EntityPlayerMPFake)serverPlayerEntity_3).kill(new TranslatableComponent("multiplayer.disconnect.duplicate_login"));
                continue;
            }
            serverPlayerEntity_3.connection.disconnect(new TranslatableComponent("multiplayer.disconnect.duplicate_login"));
        }
    }

}