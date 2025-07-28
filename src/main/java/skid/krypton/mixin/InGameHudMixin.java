package skid.krypton.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import skid.krypton.Krypton;
import skid.krypton.event.events.Render2DEvent;
import skid.krypton.manager.EventManager;
import skid.krypton.module.modules.render.HUD;

@Mixin({InGameHud.class})
public class InGameHudMixin {
    @Inject(method = {"render"}, at = {@At("HEAD")})
    private void onRenderHud(final DrawContext ctx, final RenderTickCounter rtc, final CallbackInfo ci) {
        EventManager.b(new Render2DEvent(ctx, rtc.getTickDelta(true)));
    }

    @Inject(method = {"renderStatusEffectOverlay"}, at = {@At("HEAD")}, cancellable = true)
    private void onRenderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HUD hudModule = (HUD) Krypton.INSTANCE.getModuleManager().getModule(HUD.class);
        if (hudModule != null && hudModule.isEnabled()) {
            ci.cancel();
        }
    }
}