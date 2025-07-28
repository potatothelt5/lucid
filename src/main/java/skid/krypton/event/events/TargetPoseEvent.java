package skid.krypton.event.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import skid.krypton.event.CancellableEvent;

public class TargetPoseEvent extends CancellableEvent {
    public Entity entity;
    public CallbackInfoReturnable<EntityPose> cir;

    public TargetPoseEvent(final Entity entity, final CallbackInfoReturnable<EntityPose> cir) {
        this.entity = entity;
        this.cir = cir;
    }
}