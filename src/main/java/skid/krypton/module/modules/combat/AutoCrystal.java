package skid.krypton.module.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.MinMaxSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.module.setting.Setting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.RenderUtils3D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoCrystal extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final MinMaxSetting delay;
    private final BooleanSetting render;
    private final BooleanSetting inAir;
    private final BooleanSetting pauseOnKill;
    private final BooleanSetting headBobbing;
    private final BooleanSetting rightClick;
    
    // Color settings
    private final NumberSetting outlineRed;
    private final NumberSetting outlineGreen;
    private final NumberSetting outlineBlue;
    private final NumberSetting outlineAlpha;
    private final NumberSetting fillRed;
    private final NumberSetting fillGreen;
    private final NumberSetting fillBlue;
    private final NumberSetting fillAlpha;

    private long lastBreakTime = 0;
    private long lastPlaceTime = 0;
    private long lastRenderUpdate = 0;

    private BlockPos currentObsidianPos = null;
    private BlockPos targetObsidianPos = null;
    private BlockPos lastObsidianPos = null;
    private boolean isRightClickHeld = false;
    private boolean wasRightClickHeld = false;
    private List<PlayerEntity> deadPlayers = new ArrayList<>();

    private boolean isFadingOut = false;
    private long fadeOutStartTime = 0;
    private static final long FADE_OUT_DURATION = 2000;
    private float fadeOutFactor = 1.0f;

    private double renderX = 0;
    private double renderY = 0;
    private double renderZ = 0;
    private static final double RENDER_SPEED = 0.12;



    public AutoCrystal() {
        super(EncryptedString.of("Auto Crystal"), EncryptedString.of("Automatically places and breaks end crystals"), -1, Category.COMBAT);

        rightClick = new BooleanSetting(EncryptedString.of("Right Click"), false);
        delay = new MinMaxSetting(EncryptedString.of("Delay"), 0, 200, 1, 10, 35);
        render = new BooleanSetting(EncryptedString.of("Render"), true);
        headBobbing = new BooleanSetting(EncryptedString.of("Head Bobbing"), false);
        inAir = new BooleanSetting(EncryptedString.of("In Air"), false);
        pauseOnKill = new BooleanSetting(EncryptedString.of("Pause On Kill"), false);
        
        // Initialize color settings
        outlineRed = new NumberSetting(EncryptedString.of("Outline Red"), 0.0, 255.0, 124.0, 1.0);
        outlineGreen = new NumberSetting(EncryptedString.of("Outline Green"), 0.0, 255.0, 126.0, 1.0);
        outlineBlue = new NumberSetting(EncryptedString.of("Outline Blue"), 0.0, 255.0, 238.0, 1.0);
        outlineAlpha = new NumberSetting(EncryptedString.of("Outline Alpha"), 0.0, 255.0, 255.0, 1.0);
        fillRed = new NumberSetting(EncryptedString.of("Fill Red"), 0.0, 255.0, 124.0, 1.0);
        fillGreen = new NumberSetting(EncryptedString.of("Fill Green"), 0.0, 255.0, 126.0, 1.0);
        fillBlue = new NumberSetting(EncryptedString.of("Fill Blue"), 0.0, 255.0, 238.0, 1.0);
        fillAlpha = new NumberSetting(EncryptedString.of("Fill Alpha"), 0.0, 255.0, 100.0, 1.0);

        addSetting(rightClick);
        addSetting(delay);
        addSetting(render);
        addSetting(headBobbing);
        addSetting(inAir);
        addSetting(pauseOnKill);
        addSetting(outlineRed);
        addSetting(outlineGreen);
        addSetting(outlineBlue);
        addSetting(outlineAlpha);
        addSetting(fillRed);
        addSetting(fillGreen);
        addSetting(fillBlue);
        addSetting(fillAlpha);

    }

    @Override
    public void onEnable() {
        lastBreakTime = System.currentTimeMillis();
        lastPlaceTime = System.currentTimeMillis();
        currentObsidianPos = null;
        targetObsidianPos = null;
        deadPlayers = new ArrayList<>();
    }

    @Override
    public void onDisable() {
        currentObsidianPos = null;
        targetObsidianPos = null;
        isRightClickHeld = false;
        wasRightClickHeld = false;
    }

    @EventListener
    public void onRender3D(skid.krypton.event.events.Render3DEvent event) {
        if (!render.getValue() || mc.player == null || mc.world == null) {
            return;
        }
        
        // Performance mode: disable render if enabled
        for (Setting setting : getSettings()) {
            if (setting instanceof BooleanSetting && setting.getName().equals("Disable Render")) {
                if (((BooleanSetting) setting).getValue()) {
                    return;
                }
                break;
            }
        }
        
        // Cache the render target to avoid repeated calculations
        BlockPos renderTarget = targetObsidianPos != null ? targetObsidianPos : lastObsidianPos;
        if (renderTarget == null) return;
        
        // Check if player is holding crystals and obsidian
        if (!isHoldingCrystalAndObsidian()) {
            return;
        }

        // Early exit if no target to render
        if (targetObsidianPos == null && (!isFadingOut || fadeOutFactor <= 0)) {
            return;
        }

        // Initialize position once
        if (currentObsidianPos == null) {
            currentObsidianPos = renderTarget;
            renderX = currentObsidianPos.getX();
            renderY = currentObsidianPos.getY();
            renderZ = currentObsidianPos.getZ();
        }

        // Get render quality setting
        int renderQuality = 1;
        for (Setting setting : getSettings()) {
            if (setting instanceof NumberSetting && setting.getName().equals("Render Quality")) {
                renderQuality = (int)((NumberSetting) setting).getValue();
                break;
            }
        }
        
        // Update position based on quality setting
        long currentTime = System.currentTimeMillis();
        int updateInterval = renderQuality == 1 ? 32 : renderQuality == 2 ? 48 : 64; // Higher quality = less frequent updates
        
        if (currentTime - lastRenderUpdate > updateInterval) {
            double targetX = renderTarget.getX();
            double targetY = renderTarget.getY();
            double targetZ = renderTarget.getZ();

            renderX += (targetX - renderX) * RENDER_SPEED;
            renderY += (targetY - renderY) * RENDER_SPEED;
            renderZ += (targetZ - renderZ) * RENDER_SPEED;

            lastRenderUpdate = currentTime;
        }

        MatrixStack matrices = event.matrixStack;
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d vec = cam.getPos();

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180F));
        matrices.translate(-vec.x, -vec.y, -vec.z);

        // Create colors once per frame
        Color fillColor = applyFadeOutToColor(new Color(
            (int)fillRed.getValue(),
            (int)fillGreen.getValue(),
            (int)fillBlue.getValue(),
            (int)fillAlpha.getValue()
        ));
        Color outlineColor = applyFadeOutToColor(new Color(
            (int)outlineRed.getValue(),
            (int)outlineGreen.getValue(),
            (int)outlineBlue.getValue(),
            (int)outlineAlpha.getValue()
        ));

        RenderSystem.disableDepthTest();
        
        // Render both boxes in one pass
        RenderUtils3D.renderFilledBox(matrices,
                (float)renderX,
                (float)renderY,
                (float)renderZ,
                (float)renderX + 1.0f,
                (float)renderY + 1.0f,
                (float)renderZ + 1.0f,
                fillColor
        );

        RenderUtils3D.renderBoxOutline(matrices,
                (float)renderX,
                (float)renderY,
                (float)renderZ,
                (float)renderX + 1.0f,
                (float)renderY + 1.0f,
                (float)renderZ + 1.0f,
                outlineColor,
                3.0f
        );

        RenderSystem.enableDepthTest();
        matrices.pop();
    }

    private Color applyFadeOutToColor(Color originalColor) {
        if (!isFadingOut) return originalColor;

        int alpha = (int)(originalColor.getAlpha() * fadeOutFactor);
        return new Color(
                originalColor.getRed(),
                originalColor.getGreen(),
                originalColor.getBlue(),
                alpha
        );
    }

    private void updateFadeOutFactor() {
        if (!isFadingOut) return;

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - fadeOutStartTime;

        if (elapsedTime >= FADE_OUT_DURATION) {
            fadeOutFactor = 0.0f;
            isFadingOut = false;
            targetObsidianPos = null;
        } else {
            fadeOutFactor = 1.0f - ((float) elapsedTime / FADE_OUT_DURATION);
        }
    }

    @EventListener
    public void onUpdate(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || !mc.isWindowFocused()) {
            return;
        }

        updateFadeOutFactor();

        if (render.getValue()) {
            if (isLookingAtObsidian()) {
                HitResult hitResult = mc.crosshairTarget;
                if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hitResult;
                    targetObsidianPos = blockHit.getBlockPos();

                    lastObsidianPos = targetObsidianPos;

                    isFadingOut = false;
                    fadeOutFactor = 1.0f;
                } else if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) hitResult;
                    Entity entity = entityHit.getEntity();

                    if (entity instanceof EndCrystalEntity) {
                        EndCrystalEntity crystal = (EndCrystalEntity) entity;

                        BlockPos blockPos = new BlockPos(
                                (int) Math.floor(crystal.getX()),
                                (int) Math.floor(crystal.getY()) - 1,
                                (int) Math.floor(crystal.getZ())
                        );

                        targetObsidianPos = blockPos;
                        lastObsidianPos = blockPos;

                        isFadingOut = false;
                        fadeOutFactor = 1.0f;
                    }
                }
            } else if (targetObsidianPos != null && !isFadingOut) {
                isFadingOut = true;
                fadeOutStartTime = System.currentTimeMillis();
            }
        }

        if (rightClick.getValue()) {
            boolean isRightClicking = mc.options.useKey.isPressed();

            boolean wasHeld = wasRightClickHeld;
            wasRightClickHeld = isRightClicking;

            if (!isRightClicking) {
                return;
            }

            if (!wasHeld) {
                return;
            }
        }

        if (pauseOnKill.getValue() && checkForDeadPlayers()) {
            return;
        }

        event.cancel();

        if (mc.player.isUsingItem() || (!mc.player.isOnGround() && !inAir.getValue()) ||
                System.currentTimeMillis() - lastBreakTime < delay.getRandomIntInRange()) {
            return;
        }

        if (new Random().nextFloat() < 0.05f) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        lastBreakTime = currentTime;

        HitResult hitResult = mc.crosshairTarget;
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();

            if (entity instanceof EndCrystalEntity || entity instanceof SlimeEntity || entity instanceof MagmaCubeEntity) {
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        } else if (headBobbing.getValue()) {
            BlockPos blockPos = targetObsidianPos;
            if (blockPos == null && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                blockPos = ((BlockHitResult) hitResult).getBlockPos();
            }

            if (blockPos != null) {
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof EndCrystalEntity) {
                        EndCrystalEntity crystal = (EndCrystalEntity) entity;
                        if (crystal.getPos().distanceTo(blockPos.up().toCenterPos()) < 1 &&
                                mc.player.distanceTo(crystal) <= 4.5f) {

                            float randomX = (new Random().nextFloat() * 0.6f) - 0.3f;
                            float randomY = (new Random().nextFloat() * 0.4f) + 0.25f;
                            float randomZ = (new Random().nextFloat() * 0.6f) - 0.3f;

                            mc.interactionManager.attackEntity(mc.player, crystal);
                            mc.player.swingHand(Hand.MAIN_HAND);
                            return;
                        }
                    }
                }
            }
        }

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();

            if (mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK ||
                    mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) {

                BlockPos placePos = blockPos.up();

                boolean hasCollision = false;
                for (Entity entity : mc.world.getEntities()) {
                    if (entity.getBoundingBox().intersects(
                            placePos.getX(), placePos.getY(), placePos.getZ(),
                            placePos.getX() + 1, placePos.getY() + 1, placePos.getZ() + 1)) {
                        hasCollision = true;
                        break;
                    }
                }

                if (hasCollision) {
                    return;
                }

                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof EndCrystalEntity) {
                        EndCrystalEntity crystal = (EndCrystalEntity) entity;
                        if (crystal.getPos().distanceTo(placePos.toCenterPos()) < 0.5) {
                            return;
                        }
                    }
                }

                if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL ||
                        mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {

                    Hand hand = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL ?
                            Hand.MAIN_HAND : Hand.OFF_HAND;

                    mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);
                    mc.player.swingHand(hand);
                    lastPlaceTime = currentTime;
                }
            }
        }
    }

    private boolean checkForDeadPlayers() {
        if (mc.world == null) return false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && player.getHealth() <= 0) {
                if (!deadPlayers.contains(player)) {
                    deadPlayers.add(player);
                    return true;
                }
            }
        }

        deadPlayers.removeIf(player -> player.getHealth() > 0);

        return false;
    }

    private boolean isHoldingCrystalAndObsidian() {
        if (mc.player == null) return false;
        
        boolean hasCrystal = false;
        boolean hasObsidian = false;
        
        // Check main hand and offhand first (most common)
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();
        
        if (mainHand.getItem() == Items.END_CRYSTAL || offHand.getItem() == Items.END_CRYSTAL) {
            hasCrystal = true;
        }
        if (mainHand.getItem() == Items.OBSIDIAN || offHand.getItem() == Items.OBSIDIAN) {
            hasObsidian = true;
        }
        
        // Only check hotbar if we still need items
        if (!hasCrystal || !hasObsidian) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!hasCrystal && stack.getItem() == Items.END_CRYSTAL) {
                    hasCrystal = true;
                } else if (!hasObsidian && stack.getItem() == Items.OBSIDIAN) {
                    hasObsidian = true;
                }
                
                // Early exit if we found both
                if (hasCrystal && hasObsidian) break;
            }
        }
        
        return hasCrystal && hasObsidian;
    }
    
    private boolean isLookingAtObsidian() {
        if (mc.player == null || mc.world == null) return false;

        HitResult hitResult = mc.crosshairTarget;
        if (hitResult == null) return false;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHit.getBlockPos();

            return mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN;
        }

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            Entity entity = entityHit.getEntity();

            if (entity instanceof EndCrystalEntity) {
                EndCrystalEntity crystal = (EndCrystalEntity) entity;

                BlockPos blockPos = new BlockPos(
                        (int) Math.floor(crystal.getX()),
                        (int) Math.floor(crystal.getY()) - 1,
                        (int) Math.floor(crystal.getZ())
                );

                return mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN;
            }
        }

        return false;
    }

    @EventListener
    public void onRender2D(skid.krypton.event.events.Render2DEvent event) {
    }
} 