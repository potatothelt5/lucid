package skid.krypton.module.modules.misc;

import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.utils.ChatUtils;
import skid.krypton.utils.EncryptedString;

public final class AutoLog extends Module {
    private final BooleanSetting smartToggle = new BooleanSetting(EncryptedString.of("Smart Toggle"), false)
            .setDescription(EncryptedString.of("Disables Auto Log after a low-health logout. WILL re-enable once you heal."));
    private final BooleanSetting toggleAutoReconnect = new BooleanSetting(EncryptedString.of("Toggle Auto Reconnect"), true)
            .setDescription(EncryptedString.of("Whether to disable Auto Reconnect after a logout."));

    private boolean healthListenerActive = false;

    public AutoLog() {
        super(EncryptedString.of("Auto Log"), EncryptedString.of("Automatically disconnects you when certain requirements are met."), -1, Category.MISC);
        addSettings(smartToggle, toggleAutoReconnect);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        healthListenerActive = false;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        float playerHealth = mc.player.getHealth();
        if (playerHealth <= 0) {
            this.toggle();
            return;
        }

        // Health-based disconnection logic
        if (playerHealth <= 6) { // Default health threshold
            disconnect("Health was lower than 6.");
            if (smartToggle.getValue()) {
                if (isEnabled()) this.toggle();
                enableHealthListener();
            }
            return;
        }
    }

    @EventListener
    public void onHealthTick(TickEvent event) {
        if (!healthListenerActive) return;

        if (isEnabled()) {
            healthListenerActive = false;
            return;
        }

        if (mc.player != null && !mc.player.isDead() && mc.player.getHealth() > 6) {
            ChatUtils.info("Player health greater than minimum, re-enabling module.");
            this.toggle();
            healthListenerActive = false;
        }
    }

    private void disconnect(String reason) {
        ChatUtils.warning("Disconnecting: " + reason);
        
        // Check if AutoReconnect module exists and disable it if toggleAutoReconnect is enabled
        if (toggleAutoReconnect.getValue()) {
            try {
                AutoReconnect autoReconnect = (AutoReconnect) Krypton.INSTANCE.getModuleManager().getModule(AutoReconnect.class);
                if (autoReconnect != null && autoReconnect.isEnabled()) {
                    autoReconnect.toggle();
                    ChatUtils.info("AutoReconnect was disabled");
                }
            } catch (Exception ignored) {
                // AutoReconnect module not found, continue with normal disconnect
            }
        }

        // Create disconnect packet
        MutableText text = Text.literal("[AutoLog] ");
        text.append(Text.literal(reason));
        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(text));
    }

    private void enableHealthListener() {
        healthListenerActive = true;
    }
} 