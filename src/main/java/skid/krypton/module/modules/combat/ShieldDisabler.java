package skid.krypton.module.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.AttackEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.InventoryUtil;
import skid.krypton.utils.WorldUtils;

public final class ShieldDisabler extends Module {
    private final NumberSetting hitDelay = new NumberSetting(EncryptedString.of("Hit Delay"), 0, 20, 0, 1);
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 20, 0, 1);
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
    private final BooleanSetting stun = new BooleanSetting(EncryptedString.of("Stun"), false);
    private final BooleanSetting clickSimulate = new BooleanSetting(EncryptedString.of("Click Simulation"), false);
    private final BooleanSetting requireHoldAxe = new BooleanSetting(EncryptedString.of("Hold Axe"), false);

    int previousSlot, hitClock, switchClock;

    public ShieldDisabler() {
        super(EncryptedString.of("Shield Disabler"),
                EncryptedString.of("Automatically disables your opponents shield"),
                -1,
                Category.COMBAT);

        addSettings(switchDelay, hitDelay, switchBack, stun, clickSimulate, requireHoldAxe);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        hitClock = hitDelay.getIntValue();
        switchClock = switchDelay.getIntValue();
        previousSlot = -1;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null)
            return;

        if(requireHoldAxe.getValue() && !(mc.player.getMainHandStack().getItem() instanceof AxeItem))
            return;

        if (mc.crosshairTarget instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();

            if (mc.player.isUsingItem())
                return;

            if (entity instanceof PlayerEntity player) {
                if (WorldUtils.isShieldFacingAway(player))
                    return;

                if (player.isHolding(Items.SHIELD) && player.isBlocking()) {
                    if (switchClock > 0) {
                        if (previousSlot == -1)
                            previousSlot = mc.player.getInventory().selectedSlot;

                        switchClock--;
                        return;
                    }

                    if (InventoryUtil.swap(Items.DIAMOND_AXE) || InventoryUtil.swap(Items.NETHERITE_AXE) || InventoryUtil.swap(Items.IRON_AXE) || InventoryUtil.swap(Items.GOLDEN_AXE) || InventoryUtil.swap(Items.STONE_AXE) || InventoryUtil.swap(Items.WOODEN_AXE)) {
                        if (hitClock > 0) {
                            hitClock--;
                        } else {
                            if (clickSimulate.getValue())
                                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

                            WorldUtils.hitEntity(player, true);

                            if (stun.getValue()) {
                                if (clickSimulate.getValue())
                                    mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

                                WorldUtils.hitEntity(player, true);
                            }

                            hitClock = hitDelay.getIntValue();
                            switchClock = switchDelay.getIntValue();
                        }
                    }
                } else if (previousSlot != -1) {
                    if (switchBack.getValue())
                        InventoryUtil.swap(previousSlot);

                    previousSlot = -1;
                }
            }
        }
    }

    @EventListener
    public void onAttack(AttackEvent event) {
        if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            event.cancel();
    }
} 