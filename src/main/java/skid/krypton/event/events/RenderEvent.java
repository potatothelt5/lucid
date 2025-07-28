package skid.krypton.event.events;

import net.minecraft.client.util.math.MatrixStack;
import skid.krypton.event.Event;

/**
 * Event fired during the world rendering phase.
 * This event allows modules to render custom elements in the 3D world space.
 * 
 * This event is fired during world rendering and allows modules to:
 * - Render ESP boxes around entities
 * - Draw tracers to entities
 * - Render custom 3D elements in the world
 * - Add visual overlays to the game world
 */
public class RenderEvent implements Event {
    
    private final MatrixStack matrices;
    private final float partialTicks;
    
    /**
     * Creates a new RenderEvent.
     * @param matrices The matrix stack for transformations
     * @param partialTicks The partial tick time for smooth interpolation
     */
    public RenderEvent(MatrixStack matrices, float partialTicks) {
        this.matrices = matrices;
        this.partialTicks = partialTicks;
    }
    
    /**
     * Gets the matrix stack for rendering transformations.
     * @return the matrix stack
     */
    public MatrixStack getMatrices() {
        return matrices;
    }
    
    /**
     * Gets the partial tick time for smooth interpolation.
     * @return the partial tick time
     */
    public float getPartialTicks() {
        return partialTicks;
    }
    
    @Override
    public String toString() {
        return "RenderEvent{partialTicks=" + partialTicks + ", cancelled=" + isCancelled() + "}";
    }
}