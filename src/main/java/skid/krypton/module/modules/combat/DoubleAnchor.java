package skid.krypton.module.modules.combat;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BindSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.BlockUtil;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.InventoryUtil;
import skid.krypton.utils.KeyUtils;

public final class DoubleAnchor extends Module {
    private final BindSetting activateKey = new BindSetting(EncryptedString.of("Activate Key"), 71, false).setDescription(EncryptedString.of("Key that starts double anchoring"));
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0.0, 20.0, 0.0, 1.0);
    private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), 1.0, 9.0, 1.0, 1.0);
    private int delayCounter = 0;
    private int step = 0;
    private boolean isAnchoring = false;

    public DoubleAnchor() {
        super(EncryptedString.of("Double Anchor"), EncryptedString.of("Automatically Places 2 anchors"), -1, Category.COMBAT);
        this.addSettings(this.switchDelay, this.totemSlot, this.activateKey);
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
        if (this.mc.currentScreen != null) {
            return;
        }
        if (this.mc.player == null) {
            return;
        }
        if (!this.hasRequiredItems()) {
            return;
        }
        if (!this.isAnchoring && !this.checkActivationKey()) {
            return;
        }
        final HitResult crosshairTarget = this.mc.crosshairTarget;
        if (!(this.mc.crosshairTarget instanceof BlockHitResult) || BlockUtil.isBlockAtPosition(((BlockHitResult) crosshairTarget).getBlockPos(), Blocks.AIR)) {
            this.isAnchoring = false;
            this.resetState();
            return;
        }
        if (this.delayCounter < this.switchDelay.getIntValue()) {
            ++this.delayCounter;
            return;
        }
        if (this.step == 0) {
            InventoryUtil.swap(Items.RESPAWN_ANCHOR);
        } else if (this.step == 1) {
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 2) {
            InventoryUtil.swap(Items.GLOWSTONE);
        } else if (this.step == 3) {
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 4) {
            InventoryUtil.swap(Items.RESPAWN_ANCHOR);
        } else if (this.step == 5) {
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 6) {
            InventoryUtil.swap(Items.GLOWSTONE);
        } else if (this.step == 7) {
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 8) {
            InventoryUtil.swap(this.totemSlot.getIntValue() - 1);
        } else if (this.step == 9) {
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 10) {
            this.isAnchoring = false;
            this.step = 0;
            this.resetState();
            return;
        }
        ++this.step;
    }

    private boolean hasRequiredItems() {
        boolean b = false;
        boolean b2 = false;
        for (int i = 0; i < 9; ++i) {
            final ItemStack getStack = this.mc.player.getInventory().getStack(i);
            if (getStack.getItem().equals(Items.RESPAWN_ANCHOR)) {
                b = true;
            }
            if (getStack.getItem().equals(Items.GLOWSTONE)) {
                b2 = true;
            }
        }
        return b && b2;
    }

    private boolean checkActivationKey() {
        final int d = this.activateKey.getValue();
        if (d == -1 || !KeyUtils.isKeyPressed(d)) {
            this.resetState();
            return false;
        }
        return this.isAnchoring = true;
    }

    private void resetState() {
        this.delayCounter = 0;
    }

    public boolean isAnchoringActive() {
        return this.isAnchoring;
    }
}
