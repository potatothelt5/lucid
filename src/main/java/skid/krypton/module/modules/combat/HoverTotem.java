package skid.krypton.module.modules.combat;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.mixin.HandledScreenMixin;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;

public final class HoverTotem extends Module {
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 20, 0, 1);
    private final BooleanSetting hotbar = new BooleanSetting(EncryptedString.of("Hotbar"), true).setDescription(EncryptedString.of("Puts a totem in your hotbar as well, if enabled (Setting below will work if this is enabled)"));
    private final NumberSetting slot = new NumberSetting(EncryptedString.of("Totem Slot"), 1, 9, 1, 1)
            .getValue(EncryptedString.of("Your preferred totem slot"));
    private final BooleanSetting autoSwitch = new BooleanSetting(EncryptedString.of("Auto Switch"), false)
            .setDescription(EncryptedString.of("Switches to totem slot when going inside the inventory"));

    private int clock;

    public HoverTotem() {
        super(EncryptedString.of("Hover Totem"),
                EncryptedString.of("Equips a totem in your totem and offhand slots if a totem is hovered"),
                -1,
                Category.COMBAT);
        addSettings(delay, hotbar, slot, autoSwitch);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clock = 0;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.currentScreen instanceof InventoryScreen inv) {
            Slot hoveredSlot = ((HandledScreenMixin) inv).getFocusedSlot();

            if (autoSwitch.getValue())
                mc.player.getInventory().selectedSlot = slot.getIntValue() - 1;

            if (hoveredSlot != null) {
                int slot = hoveredSlot.getIndex();

                if (slot > 35)
                    return;

                int totem = this.slot.getIntValue() - 1;

                if (hoveredSlot.getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                    if (hotbar.getValue() && mc.player.getInventory().getStack(totem).getItem() != Items.TOTEM_OF_UNDYING) {
                        if (clock > 0) {
                            clock--;
                            return;
                        }

                        mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, slot, totem, SlotActionType.SWAP, mc.player);
                        clock = delay.getIntValue();
                    } else if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        if (clock > 0) {
                            clock--;
                            return;
                        }

                        mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, slot, 40, SlotActionType.SWAP, mc.player);
                        clock = delay.getIntValue();
                    }
                }
            }
        } else clock = delay.getIntValue();
    }
}
