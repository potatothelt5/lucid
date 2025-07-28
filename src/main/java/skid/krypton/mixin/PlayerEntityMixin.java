package skid.krypton.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import skid.krypton.Krypton;
import skid.krypton.module.modules.combat.NoHitDelay;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    
    @Inject(method = "getAttackCooldownProgress", at = @At("HEAD"), cancellable = true)
    private void onGetAttackCooldownProgress(float baseTime, CallbackInfoReturnable<Float> cir) {
        NoHitDelay noHitDelay = (NoHitDelay) Krypton.INSTANCE.getModuleManager().getModule(NoHitDelay.class);
        if (noHitDelay != null && noHitDelay.isEnabled()) {
            if (noHitDelay.instantAttack.getValue()) {
                cir.setReturnValue(1.0f);
            } else if (noHitDelay.cooldownValue.getValue() < 1.0) {
                cir.setReturnValue((float) noHitDelay.cooldownValue.getValue());
            }
        }
    }
} 