package skid.krypton.utils;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class FakeInvScreen extends InventoryScreen {
    
    public FakeInvScreen(PlayerEntity playerEntity) {
        super(playerEntity);
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        // Override to prevent default behavior
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false; // Prevent mouse clicks from being processed
    }
}
