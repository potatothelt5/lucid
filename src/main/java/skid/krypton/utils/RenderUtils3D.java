package skid.krypton.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

/**
 * Utility class for 3D world rendering operations.
 * Provides methods for rendering boxes, lines, and other 3D elements in world space.
 */
public class RenderUtils3D {
    
    /**
     * Renders a filled box in 3D world space.
     * @param matrices The matrix stack for transformations
     * @param x1 The minimum X coordinate
     * @param y1 The minimum Y coordinate
     * @param z1 The minimum Z coordinate
     * @param x2 The maximum X coordinate
     * @param y2 The maximum Y coordinate
     * @param z2 The maximum Z coordinate
     * @param color The color to render the box
     */
    public static void renderFilledBox(MatrixStack matrices, float x1, float y1, float z1, 
                                     float x2, float y2, float z2, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float alpha = color.getAlpha() / 255f;
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, 
                VertexFormats.POSITION_COLOR);
        
        // Bottom face
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha);
        
        // Top face
        buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha);
        
        // North face
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha);
        
        // South face
        buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha);
        
        // East face
        buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha);
        
        // West face
        buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    
    /**
     * Renders a line between two points in 3D world space.
     * @param matrices The matrix stack for transformations
     * @param color The color of the line
     * @param start The starting position
     * @param end The ending position
     */
    public static void renderLine(MatrixStack matrices, Color color, Vec3d start, Vec3d end) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float alpha = color.getAlpha() / 255f;
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        
        GL11.glLineWidth(2.0f);
        
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrix, (float) start.x, (float) start.y, (float) start.z)
              .color(red, green, blue, alpha);
        buffer.vertex(matrix, (float) end.x, (float) end.y, (float) end.z)
              .color(red, green, blue, alpha);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        GL11.glLineWidth(1.0f);
        RenderSystem.disableBlend();
    }
    
    /**
     * Renders an outlined box in 3D world space.
     * @param matrices The matrix stack for transformations
     * @param x1 The minimum X coordinate
     * @param y1 The minimum Y coordinate
     * @param z1 The minimum Z coordinate
     * @param x2 The maximum X coordinate
     * @param y2 The maximum Y coordinate
     * @param z2 The maximum Z coordinate
     * @param color The color to render the outline
     * @param lineWidth The width of the outline lines
     */
    public static void renderBoxOutline(MatrixStack matrices, float x1, float y1, float z1,
                                      float x2, float y2, float z2, Color color, float lineWidth) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float alpha = color.getAlpha() / 255f;
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        
        GL11.glLineWidth(lineWidth);
        
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION_COLOR);
        
        // Bottom edges
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        
        // Top edges
        buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha);
        
        // Vertical edges
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        GL11.glLineWidth(1.0f);
        RenderSystem.disableBlend();
    }
}