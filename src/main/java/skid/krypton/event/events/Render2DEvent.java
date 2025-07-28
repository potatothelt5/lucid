package skid.krypton.event.events;

import net.minecraft.client.gui.DrawContext;
import skid.krypton.event.CancellableEvent;

public class Render2DEvent extends CancellableEvent {
    public DrawContext context;
    public float tickDelta;

    public Render2DEvent(final DrawContext context, final float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
    }
}