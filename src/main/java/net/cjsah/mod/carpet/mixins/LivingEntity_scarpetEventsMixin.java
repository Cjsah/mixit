package net.cjsah.mod.carpet.mixins;

import net.cjsah.mod.carpet.fakes.EntityInterface;
import net.cjsah.mod.carpet.fakes.LivingEntityInterface;
import net.cjsah.mod.carpet.script.EntityEventsGroup;
import net.cjsah.mod.carpet.script.CarpetEventServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_scarpetEventsMixin extends Entity implements LivingEntityInterface
{

    @Shadow protected abstract void jump();

    @Shadow protected boolean jumping;

    public LivingEntity_scarpetEventsMixin(EntityType<?> type, Level world)
    {
        super(type, world);
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeathCall(DamageSource damageSource_1, CallbackInfo ci)
    {
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DEATH, damageSource_1.msgId);
    }

    @Inject(method = "applyDamage", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;applyArmorToDamage(Lnet/minecraft/entity/damage/DamageSource;F)F",
            shift = At.Shift.BEFORE
    ))
    private void entityTakingDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DAMAGE, amount, source);
        // this is not applicable since its not a playr for sure
        //if (entity instanceof ServerPlayerEntity && PLAYER_TAKES_DAMAGE.isNeeded())
        //{
        //    PLAYER_TAKES_DAMAGE.onDamage(entity, float_2, damageSource_1);
        //}
        if (source.getEntity() instanceof ServerPlayer && CarpetEventServer.Event.PLAYER_DEALS_DAMAGE.isNeeded())
        {
            CarpetEventServer.Event.PLAYER_DEALS_DAMAGE.onDamage(this, amount, source);
        }
    }

    @Override
    public void doJumpCM()
    {
        jump();
    }

    @Override
    public boolean isJumpingCM()
    {
        return jumping;
    }
}