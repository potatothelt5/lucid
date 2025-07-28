package skid.krypton.module.modules.misc;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BindSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.KeyUtils;

public class AutoLoot extends Module {
    private final NumberSetting minTotems = new NumberSetting(EncryptedString.of("Totems To Keep"), 0.0, 36.0, 2.0, 1.0);
    private final NumberSetting minPearls = new NumberSetting(EncryptedString.of("Pearls To Keep"), 0.0, 576.0, 16.0, 1.0);
    private final NumberSetting minCrystals = new NumberSetting(EncryptedString.of("Crystals To Keep"), 0.0, 64.0, 16.0, 1.0);
    private final NumberSetting dropInterval = new NumberSetting(EncryptedString.of("Dropping Speed"), 0.0, 10.0, 0.0, 1.0);
    private final BindSetting activateKey = new BindSetting(EncryptedString.of("Binding"), -1, false);

    private int dropClock = 0;

    public AutoLoot() {
        super(EncryptedString.of("Auto Loot"), EncryptedString.of("Helps you loot kills"), -1, Category.MISC);
        this.addSettings(minTotems, minPearls, minCrystals, dropInterval, activateKey);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.dropClock = 0;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        final PlayerInventory inv = mc.player.getInventory();
        if (this.dropClock != 0) {
            --this.dropClock;
            return;
        }

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            return;
        }

        if (!KeyUtils.isKeyPressed(activateKey.getValue())) {
            return;
        }

        final Screen screen = mc.currentScreen;
        final HandledScreen<?> gui = (HandledScreen<?>) screen;
        final Slot slot = getSlotUnderMouse(gui);
        if (slot == null) {
            return;
        }

        final int slotUnderMouse = slot.getIndex();
        if (slotUnderMouse > 35 || slotUnderMouse < 9) {
            return;
        }

        ItemStack stack = inv.main.get(slotUnderMouse);
        if (stack.isEmpty()) return;

        // Handle Totems
        if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            if (getItemCount(Items.TOTEM_OF_UNDYING) <= this.minTotems.getIntValue()) {
                return;
            }
            mc.interactionManager.clickSlot(
                ((PlayerScreenHandler) ((InventoryScreen) mc.currentScreen).getScreenHandler()).syncId,
                slotUnderMouse, 1, SlotActionType.THROW, mc.player
            );
            this.dropClock = this.dropInterval.getIntValue();
            return;
        }

        // Handle Pearls
        if (stack.getItem() == Items.ENDER_PEARL) {
            if (getItemCount(Items.ENDER_PEARL) <= this.minPearls.getIntValue()) {
                return;
            }
            mc.interactionManager.clickSlot(
                ((PlayerScreenHandler) ((InventoryScreen) mc.currentScreen).getScreenHandler()).syncId,
                slotUnderMouse, 1, SlotActionType.THROW, mc.player
            );
            this.dropClock = this.dropInterval.getIntValue();
            return;
        }

        // Handle Crystals
        if (stack.getItem() == Items.END_CRYSTAL) {
            if (getItemCount(Items.END_CRYSTAL) <= this.minCrystals.getIntValue()) {
                return;
            }
            mc.interactionManager.clickSlot(
                ((PlayerScreenHandler) ((InventoryScreen) mc.currentScreen).getScreenHandler()).syncId,
                slotUnderMouse, 1, SlotActionType.THROW, mc.player
            );
            this.dropClock = this.dropInterval.getIntValue();
            return;
        }

        // Handle Dirt
        if (stack.getItem() == Items.DIRT) {
            mc.interactionManager.clickSlot(
                ((PlayerScreenHandler) ((InventoryScreen) mc.currentScreen).getScreenHandler()).syncId,
                slotUnderMouse, 1, SlotActionType.THROW, mc.player
            );
            this.dropClock = this.dropInterval.getIntValue();
            return;
        }

        // Handle Cobblestone
        if (stack.getItem() == Items.COBBLESTONE) {
            mc.interactionManager.clickSlot(
                ((PlayerScreenHandler) ((InventoryScreen) mc.currentScreen).getScreenHandler()).syncId,
                slotUnderMouse, 1, SlotActionType.THROW, mc.player
            );
            this.dropClock = this.dropInterval.getIntValue();
            return;
        }

        // Handle Grass Block
        if (stack.getItem() == Items.GRASS_BLOCK) {
            mc.interactionManager.clickSlot(
                ((PlayerScreenHandler) ((InventoryScreen) mc.currentScreen).getScreenHandler()).syncId,
                slotUnderMouse, 1, SlotActionType.THROW, mc.player
            );
            this.dropClock = this.dropInterval.getIntValue();
            return;
        }
    }

    private Slot getSlotUnderMouse(HandledScreen<?> screen) {
        // Simple implementation to get slot under mouse
        // This is a basic version - you might need to implement proper mouse position detection
        if (mc.mouse == null) return null;
        
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        
        for (Slot slot : screen.getScreenHandler().slots) {
            if (mouseX >= slot.x && mouseX < slot.x + 16 && 
                mouseY >= slot.y && mouseY < slot.y + 16) {
                return slot;
            }
        }
        return null;
    }
    
    private int getItemCount(net.minecraft.item.Item item) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
} 