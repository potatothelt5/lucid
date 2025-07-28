package skid.krypton.module.modules.misc;

import net.minecraft.block.BambooBlock;
import net.minecraft.block.BambooShootBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.AttackBlockEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.InventoryUtil;

import java.util.function.Predicate;

public final class AutoTool extends Module {
    private final BooleanSetting antiBreak = new BooleanSetting(EncryptedString.of("Anti Break"), true);
    private final NumberSetting antiBreakPercentage = new NumberSetting(EncryptedString.of("Anti Break Percentage"), 1.0, 100.0, 5.0, 1.0);
    private boolean isToolSwapping;
    private int keybindCounter;
    private int selectedToolSlot;

    public AutoTool() {
        super(EncryptedString.of("Auto Tool"), EncryptedString.of("Module that automatically switches to best tool"), -1, Category.MISC);
        this.addSettings(this.antiBreak, this.antiBreakPercentage);
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
        if (this.keybindCounter <= 0 && this.isToolSwapping && this.selectedToolSlot != -1) {
            InventoryUtil.swap(this.selectedToolSlot);
            this.isToolSwapping = false;
        } else {
            --this.keybindCounter;
        }
    }

    @EventListener
    public void handleAttackBlockEvent(final AttackBlockEvent attackBlockEvent) {
        final BlockState getBlockState = this.mc.world.getBlockState(attackBlockEvent.pos);
        final ItemStack getBlockEntity = this.mc.player.getMainHandStack();
        double n = -1.0;
        this.selectedToolSlot = -1;
        for (int i = 0; i < 9; ++i) {
            final double a = calculateToolEfficiency(this.mc.player.getInventory().getStack(i), getBlockState, itemStack -> !this.isToolBreakingSoon(itemStack));
            if (a >= 0.0) {
                if (a > n) {
                    this.selectedToolSlot = i;
                    n = a;
                }
            }
        }
        if ((this.selectedToolSlot != -1 && n > calculateToolEfficiency(getBlockEntity, getBlockState, itemStack2 -> !this.isToolBreakingSoon(itemStack2))) || this.isToolBreakingSoon(getBlockEntity) || !isToolItemStack(getBlockEntity)) {
            InventoryUtil.swap(this.selectedToolSlot);
        }
        final ItemStack method_8322 = this.mc.player.getMainHandStack();
        if (this.isToolBreakingSoon(method_8322) && isToolItemStack(method_8322)) {
            this.mc.options.attackKey.setPressed(false);
            attackBlockEvent.cancel();
        }
    }

    public static double calculateToolEfficiency(final ItemStack itemStack, final BlockState blockState, final Predicate<ItemStack> predicate) {
        if (!predicate.test(itemStack) || !isToolItemStack(itemStack)) {
            return -1.0;
        }
        if (!itemStack.isSuitableFor(blockState) && (!(itemStack.getItem() instanceof SwordItem) || (!(blockState.getBlock() instanceof BambooBlock) && !(blockState.getBlock() instanceof BambooShootBlock))) && (!(itemStack.getItem() instanceof ShearsItem) || !(blockState.getBlock() instanceof LeavesBlock)) && !blockState.isIn(BlockTags.WOOL)) {
            return -1.0;
        }
        return 0.0 + itemStack.getMiningSpeedMultiplier(blockState) * 1000.0f;
    }

    public static boolean isToolItemStack(final ItemStack itemStack) {
        return isToolItem(itemStack.getItem());
    }

    public static boolean isToolItem(final Item item) {
        return item instanceof ToolItem || item instanceof ShearsItem;
    }

    private boolean isToolBreakingSoon(final ItemStack itemStack) {
        return this.antiBreak.getValue() && itemStack.getMaxDamage() - itemStack.getDamage() < itemStack.getMaxDamage() * this.antiBreakPercentage.getIntValue() / 100;
    }
}
