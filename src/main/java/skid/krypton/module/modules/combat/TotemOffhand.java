package skid.krypton.module.modules.combat;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.InventoryUtil;

public final class TotemOffhand extends Module {
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 5, 0, 1);
    private final NumberSetting equipDelay = new NumberSetting(EncryptedString.of("Equip Delay"), 1, 5, 1, 1);
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), false);

    private int switchClock, equipClock, switchBackClock;
    private int previousSlot = -1;
    boolean sent, active = false;

    public TotemOffhand() {
        super(EncryptedString.of("Totem Offhand"), EncryptedString.of("Switches to your totem slot and offhands a totem if you dont have one already"), -1, Category.COMBAT);
        addSettings(switchDelay, equipDelay, switchBack);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if(mc.currentScreen != null)
            return;

        if(mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING)
            active = true;

        if(active) {
            if (switchClock < switchDelay.getIntValue()) {
                switchClock++;
                return;
            }

            if(previousSlot == -1)
                previousSlot = mc.player.getInventory().selectedSlot;

            if (InventoryUtil.swap(Items.TOTEM_OF_UNDYING)) {
                if (equipClock < equipDelay.getIntValue()) {
                    equipClock++;
                    return;
                }

                if (!sent) {
                    mc.getNetworkHandler().getConnection().send(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                    sent = true;
                    return;
                }
            }

            if(switchBackClock < switchDelay.getValue()) {
                switchBackClock++;
            } else {
                if(switchBack.getValue())
                    InventoryUtil.swap(previousSlot);

                reset();
            }
        }
    }

    public void reset() {
        switchClock = 0;
        equipClock = 0;
        switchBackClock = 0;
        previousSlot = -1;

        sent = false;
        active = false;
    }
} 