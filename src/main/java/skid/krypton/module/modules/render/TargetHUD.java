package skid.krypton.module.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.PacketSendEvent;
import skid.krypton.event.events.Render2DEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.*;

import java.awt.*;

public final class TargetHUD extends Module {
    private final NumberSetting xPosition = new NumberSetting(EncryptedString.of("X"), 0.0, 1920.0, 500.0, 1.0);
    private final NumberSetting yPosition = new NumberSetting(EncryptedString.of("Y"), 0.0, 1080.0, 500.0, 1.0);
    private final BooleanSetting timeoutEnabled = new BooleanSetting(EncryptedString.of("Timeout"), true).setDescription(EncryptedString.of("Target hud will disappear after 10 seconds"));
    private final NumberSetting fadeSpeed = new NumberSetting(EncryptedString.of("Fade Speed"), 5.0, 30.0, 15.0, 1.0).getValue(EncryptedString.of("Speed of animations"));
    private final Color primaryColor = new Color(255, 50, 100);
    private final Color backgroundColor = new Color(0, 0, 0, 175);
    private long lastAttackTime = 0L;
    public static float fadeProgress = 1.0f;
    private float currentHealth = 0.0f;
    private TargetHUDHandler hudHandler;

    public TargetHUD() {
        super(EncryptedString.of("Target HUD"), EncryptedString.of("Displays detailed information about your target with style"), -1, Category.RENDER);
        this.addSettings(this.xPosition, this.yPosition, this.timeoutEnabled, this.fadeSpeed);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventListener
    public void a(final Render2DEvent render2DEvent) {
        final DrawContext a = render2DEvent.context;
        final int f = this.xPosition.getIntValue();
        final int f2 = this.yPosition.getIntValue();
        final float g = this.fadeSpeed.getFloatValue();
        final Color h = this.primaryColor;
        final Color i = this.backgroundColor;
        RenderUtils.unscaledProjection();
        final boolean b = this.mc.player.getAttacking() != null && this.mc.player.getAttacking() instanceof PlayerEntity && this.mc.player.getAttacking().isAlive();
        final boolean b2 = !this.timeoutEnabled.getValue() || System.currentTimeMillis() - this.lastAttackTime <= 10000L;
        float n;
        if (b && b2) {
            n = 0.0f;
        } else {
            n = 1.0f;
        }
        TargetHUD.fadeProgress = RenderUtils.fast(TargetHUD.fadeProgress, n, g);
        if (TargetHUD.fadeProgress < 0.99f && b) {
            final LivingEntity getAttacking = this.mc.player.getAttacking();
            final PlayerListEntry playerListEntry = this.mc.getNetworkHandler().getPlayerListEntry(getAttacking.getUuid());
            final MatrixStack matrices = a.getMatrices();
            matrices.push();
            final float n2 = 1.0f - TargetHUD.fadeProgress;
            final float n3 = 0.8f + 0.2f * n2;
            matrices.translate((float) f, (float) f2, 0.0f);
            matrices.scale(n3, n3, 1.0f);
            matrices.translate((float) (-f), (float) (-f2), 0.0f);
            this.currentHealth = RenderUtils.fast(this.currentHealth, getAttacking.getHealth() + getAttacking.getAbsorptionAmount(), g * 0.5f);
            this.a(a, f, f2, (PlayerEntity) getAttacking, playerListEntry, n2, h, i);
            matrices.pop();
        }
        RenderUtils.scaledProjection();
    }

    private void a(final DrawContext drawContext, final int n, final int n2, final PlayerEntity playerEntity, final PlayerListEntry playerListEntry, final float n3, final Color color, final Color color2) {
        final MatrixStack matrices = drawContext.getMatrices();
        RenderUtils.renderRoundedQuad(matrices, new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (50.0f * n3)), n - 5, n2 - 5, n + 300 + 5, n2 + 180 + 5, 15.0, 15.0, 15.0, 15.0, 30.0);
        RenderUtils.renderRoundedQuad(matrices, new Color(color2.getRed(), color2.getGreen(), color2.getBlue(), (int) (color2.getAlpha() * n3)), n, n2, n + 300, n2 + 180, 10.0, 10.0, 10.0, 10.0, 20.0);
        final Color color3 = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * n3));
        RenderUtils.renderRoundedQuad(matrices, color3, n + 20, n2, n + 300 - 20, n2 + 3, 0.0, 0.0, 0.0, 0.0, 10.0);
        RenderUtils.renderRoundedQuad(matrices, color3, n + 20, n2 + 180 - 3, n + 300 - 20, n2 + 180, 0.0, 0.0, 0.0, 0.0, 10.0);
        if (playerListEntry != null) {
            RenderUtils.renderRoundedQuad(matrices, new Color(30, 30, 30, (int) (200.0f * n3)), n + 15, n2 + 15, n + 85, n2 + 85, 5.0, 5.0, 5.0, 5.0, 10.0);
            PlayerSkinDrawer.draw(drawContext, playerListEntry.getSkinTextures().texture(), n + 25, n2 + 25, 50);
            TextRenderer.drawString(playerEntity.getName().getString(), drawContext, n + 100, n2 + 25, ColorUtil.a((int) (System.currentTimeMillis() % 1000L / 1000.0f), 1).getRGB());
            TextRenderer.drawString(MathUtil.roundToNearest(playerEntity.distanceTo(this.mc.player), 1.0) + " blocks away", drawContext, n + 100, n2 + 45, Color.WHITE.getRGB());
            RenderUtils.renderRoundedQuad(matrices, new Color(60, 60, 60, (int) (200.0f * n3)), n + 15, n2 + 95, n + 300 - 15, n2 + 110, 5.0, 5.0, 5.0, 5.0, 10.0);
            final float b = this.currentHealth / playerEntity.getMaxHealth();
            final float n4 = 270.0f * Math.min(1.0f, b);
            RenderUtils.renderRoundedQuad(matrices, this.a(b * (float) (0.800000011920929 + 0.20000000298023224 * Math.sin(System.currentTimeMillis() / 300.0)), n3), n + 15, n2 + 95, n + 15 + (int) n4, n2 + 110, 5.0, 5.0, 5.0, 5.0, 10.0);
            final String s = Math.round(this.currentHealth) + "/" + Math.round(playerEntity.getMaxHealth()) + " HP";
            TextRenderer.drawString(s, drawContext, n + 15 + (int) n4 / 2 - TextRenderer.getWidth(s) / 2, n2 + 95, Color.WHITE.getRGB());
            final int n5 = n2 + 120;
            this.a(drawContext, n + 15, n5, 80, 45, "PING", playerListEntry.getLatency() + "ms", this.a(playerListEntry.getLatency(), n3), color3, n3);
            String s2;
            if (playerListEntry != null) {
                s2 = "PLAYER";
            } else {
                s2 = "BOT";
            }
            Color color4;
            if (playerListEntry != null) {
                color4 = new Color(100, 255, 100, (int) (255.0f * n3));
            } else {
                color4 = new Color(255, 100, 100, (int) (255.0f * n3));
            }
            this.a(drawContext, n + 100 + 5, n5, 80, 45, "TYPE", s2, color4, color3, n3);
            if (playerEntity.hurtTime > 0) {
                this.a(drawContext, n + 200 + 5, n5, 80, 45, "HURT", "" + playerEntity.hurtTime, this.b(playerEntity.hurtTime, n3), color3, n3);
            } else {
                this.a(drawContext, n + 200 + 5, n5, 80, 45, "HURT", "No", new Color(150, 150, 150, (int) (255.0f * n3)), color3, n3);
            }
        } else {
            TextRenderer.drawString("BOT DETECTED", drawContext, n + 150 - TextRenderer.getWidth("BOT DETECTED") / 2, n2 + 90, new Color(255, 50, 50).getRGB());
        }
    }

    private void a(final DrawContext drawContext, final int n, final int n2, final int n3, final int n4, final String s, final String s2, final Color color, final Color color2, final float n5) {
        final MatrixStack matrices = drawContext.getMatrices();
        RenderUtils.renderRoundedQuad(matrices, color2, n, n2, n + n3, n2 + 3, 3.0, 3.0, 0.0, 0.0, 6.0);
        RenderUtils.renderRoundedQuad(matrices, new Color(30, 30, 30, (int) (200.0f * n5)), n, n2 + 3, n + n3, n2 + n4, 0.0, 0.0, 3.0, 3.0, 6.0);
        TextRenderer.drawString(s, drawContext, n + n3 / 2 - TextRenderer.getWidth(s) / 2, n2 + 5, new Color(200, 200, 200, (int) (255.0f * n5)).getRGB());
        TextRenderer.drawString(s2, drawContext, n + n3 / 2 - TextRenderer.getWidth(s2) / 2, n2 + n4 - 17, color.getRGB());
    }

    private Color a(final float n, final float n2) {
        Color color;
        if (n > 0.75f) {
            color = ColorUtil.a(new Color(100, 255, 100), new Color(255, 255, 100), (1.0f - n) * 4.0f);
        } else if (n > 0.25f) {
            color = ColorUtil.a(new Color(255, 255, 100), new Color(255, 100, 100), (0.75f - n) * 2.0f);
        } else {
            float n3;
            if (n < 0.1f) {
                n3 = (float) (0.699999988079071 + 0.30000001192092896 * Math.sin(System.currentTimeMillis() / 200.0));
            } else {
                n3 = 1.0f;
            }
            color = new Color((int) (255.0f * n3), (int) (100.0f * n3), (int) (100.0f * n3));
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * n2));
    }

    private Color a(final int n, final float n2) {
        Color color;
        if (n < 50) {
            color = new Color(100, 255, 100);
        } else if (n < 100) {
            color = ColorUtil.a(new Color(100, 255, 100), new Color(255, 255, 100), (n - 50) / 50.0f);
        } else if (n < 200) {
            color = ColorUtil.a(new Color(255, 255, 100), new Color(255, 150, 50), (n - 100) / 100.0f);
        } else {
            color = ColorUtil.a(new Color(255, 150, 50), new Color(255, 80, 80), Math.min(1.0f, (n - 200) / 300.0f));
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * n2));
    }

    private Color b(final int n, final float n2) {
        final double n3 = 0.699999988079071 + 0.30000001192092896 * Math.sin(System.currentTimeMillis() / 150.0);
        final float min = Math.min(1.0f, n / 10.0f);
        final Color color = new Color(255, (int) (50.0f + 100.0f * (1.0f - min)), (int) (50.0f + 100.0f * (1.0f - min)));
        return new Color((int) (color.getRed() * (float) n3), (int) (color.getGreen() * (float) n3), (int) (color.getBlue() * (float) n3), (int) (255.0f * n2));
    }

    @EventListener
    public void a(final PacketSendEvent packetEvent) {
        if (packetEvent.packet instanceof final PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
            if (this.hudHandler == null) {
                this.hudHandler = new TargetHUDHandler(this);
            }
            if (this.hudHandler.isAttackPacket(playerInteractEntityC2SPacket)) {
                this.lastAttackTime = System.currentTimeMillis();
            }
        }
    }

    public static class TargetHUDHandler {
        public static final MinecraftClient MC = MinecraftClient.getInstance();
        final  TargetHUD this$0;

        TargetHUDHandler(final TargetHUD this$0) {
            this.this$0 = this$0;
        }

        public boolean isAttackPacket(final PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
            String string;
            try {
                string = playerInteractEntityC2SPacket.toString();
                if (string.contains("ATTACK")) {
                    return true;
                }
            } catch (final Exception ex) {
                return MC.player != null && MC.player.getAttacking() != null && MC.player.getAttacking() instanceof PlayerEntity;
            }
            try {
                if (MC.player == null || MC.player.getAttacking() == null || !(MC.player.getAttacking() instanceof PlayerEntity)) {
                    return false;
                }
                final boolean contains = string.contains(Hand.MAIN_HAND.toString());
                final boolean contains2 = string.contains("INTERACT_AT");
                if (contains && contains2) {
                    return true;
                }
            } catch (final Exception ex2) {
                return MC.player != null && MC.player.getAttacking() != null && MC.player.getAttacking() instanceof PlayerEntity;
            }
            try {
                return MC.player.handSwinging && MC.player.getAttacking() != null;
            } catch (final Exception ignored) {
            }
            return MC.player != null && MC.player.getAttacking() != null && MC.player.getAttacking() instanceof PlayerEntity;
        }
    }

}
