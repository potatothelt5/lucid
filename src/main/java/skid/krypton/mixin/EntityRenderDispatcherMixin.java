package skid.krypton.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import skid.krypton.Krypton;
import skid.krypton.module.Module;
import skid.krypton.module.modules.combat.Hitbox;

@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin {
    @ModifyVariable(method = {"renderHitbox"}, ordinal = 0, at = @At(value = "STORE", ordinal = 0))
    private static Box onRenderHitboxEditBox(final Box box) {
        final Module hitboxes = Krypton.INSTANCE.MODULE_MANAGER.getModule(Hitbox.class);
        return hitboxes.isEnabled() ? box.expand(((Hitbox) hitboxes).getHitboxExpansion()) : box;
    }
}