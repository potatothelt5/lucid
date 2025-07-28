package skid.krypton.module.modules.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.AttackEvent;
import skid.krypton.event.events.MouseButtonEvent;
import skid.krypton.event.events.Render2DEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BindSetting;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.TextRenderer;
import skid.krypton.utils.WorldUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class Friends extends Module {
    private final BindSetting addFriendKey = new BindSetting(EncryptedString.of("Friend Key"), GLFW.GLFW_MOUSE_BUTTON_MIDDLE, false);
    public final BooleanSetting antiAttack = new BooleanSetting(EncryptedString.of("Anti-Attack"), false);
    public final BooleanSetting disableAimAssist = new BooleanSetting(EncryptedString.of("Anti-Aim"), false);
    public final BooleanSetting friendStatus = new BooleanSetting(EncryptedString.of("Friend Status"), false);

    private final List<String> friends = new ArrayList<>();

    public Friends() {
        super(EncryptedString.of("Friends"), EncryptedString.of("This module makes it so you can't do certain stuff if you have a player friended!"), -1, Category.CLIENT);
        addSettings(addFriendKey, antiAttack, disableAimAssist, friendStatus);
    }

    @EventListener
    public void onMouseButton(MouseButtonEvent event) {
        if (mc.player == null || mc.currentScreen != null) return;

        if (mc.crosshairTarget instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();

            if (entity instanceof PlayerEntity player) {
                if (event.button == addFriendKey.getValue() && event.actions == GLFW.GLFW_PRESS) {
                    String playerName = player.getName().getString();
                    if (!friends.contains(playerName)) {
                        friends.add(playerName);
                        if (mc.player != null) {
                            mc.player.sendMessage(Text.of("Added " + playerName + " as friend!"));
                        }
                    } else {
                        friends.remove(playerName);
                        if (mc.player != null) {
                            mc.player.sendMessage(Text.of("Removed " + playerName + " from friends!"));
                        }
                    }
                }
            }
        }
    }

    @EventListener
    public void onAttack(AttackEvent event) {
        if (!antiAttack.getValue()) return;

        if (isAimingOverFriend()) {
            event.cancel();
        }
    }

    @EventListener
    public void onRender2D(Render2DEvent event) {
        if (!friendStatus.getValue()) return;

        if (WorldUtils.getHitResult(100) instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();

            if (entity instanceof PlayerEntity player) {
                String playerName = player.getName().getString();
                if (friends.contains(playerName)) {
                    TextRenderer.drawCenteredString(EncryptedString.of("Player is friend"), event.context, (mc.getWindow().getWidth() / 2), (mc.getWindow().getHeight() / 2) + 25, Color.GREEN.getRGB());
                }
            }
        }
    }

    public boolean isFriend(PlayerEntity player) {
        return friends.contains(player.getName().getString());
    }

    private boolean isAimingOverFriend() {
        HitResult hitResult = WorldUtils.getHitResult(100);
        if (hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof PlayerEntity player) {
                return isFriend(player);
            }
        }
        return false;
    }
}