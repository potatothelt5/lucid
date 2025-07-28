package skid.krypton.module.modules.combat;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.modules.donut.RtpBaseFinder;
import skid.krypton.module.modules.donut.TunnelBaseFinder;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;

public final class AutoTotem extends Module {
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0.0, 5.0, 1.0, 1.0);
    private int delayCounter;

    public AutoTotem() {
        super(EncryptedString.of("Auto Totem"), EncryptedString.of("Automatically holds totem in your off hand"), -1, Category.COMBAT);
        this.addSettings(this.delay);
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
    public void onTick(final TickEvent event) {
        if (this.mc.player == null) {
            return;
        }
        final Module rtpBaseFinder = Krypton.INSTANCE.MODULE_MANAGER.getModule(RtpBaseFinder.class);
        if (rtpBaseFinder.isEnabled() && ((RtpBaseFinder) rtpBaseFinder).isRepairingActive()) {
            return;
        }
        final Module tunnelBaseFinder = Krypton.INSTANCE.MODULE_MANAGER.getModule(TunnelBaseFinder.class);
        if (tunnelBaseFinder.isEnabled() && ((TunnelBaseFinder) tunnelBaseFinder).isDigging()) {
            return;
        }
        if (this.mc.player.getInventory().getStack(40).getItem() == Items.TOTEM_OF_UNDYING) {
            this.delayCounter = this.delay.getIntValue();
            return;
        }
        if (this.delayCounter > 0) {
            --this.delayCounter;
            return;
        }
        final int slot = this.findItemSlot(Items.TOTEM_OF_UNDYING);
        if (slot == -1) {
            return;
        }
        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, convertSlotIndex(slot), 40, SlotActionType.SWAP, this.mc.player);
        this.delayCounter = this.delay.getIntValue();
    }

    public int findItemSlot(final Item item) {
        if (this.mc.player == null) {
            return -1;
        }
        for (int i = 0; i < 36; ++i) {
            if (this.mc.player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private static int convertSlotIndex(final int slotIndex) {
        if (slotIndex < 9) {
            return 36 + slotIndex;
        }
        return slotIndex;
    }
}