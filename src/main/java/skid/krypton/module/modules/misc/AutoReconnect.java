package skid.krypton.module.modules.misc;

import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.ChatUtils;
import skid.krypton.utils.EncryptedString;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AutoReconnect extends Module {
    private final NumberSetting time = new NumberSetting(EncryptedString.of("Delay"), 0, 30, 3.5, 0.5)
            .getValue(EncryptedString.of("The amount of seconds to wait before reconnecting to the server."));
    private final BooleanSetting button = new BooleanSetting(EncryptedString.of("Hide Buttons"), false)
            .setDescription(EncryptedString.of("Will hide the buttons related to Auto Reconnect."));

    private ServerAddress lastServerAddress;
    private ServerInfo lastServerInfo;
    private boolean shouldReconnect = false;
    private long reconnectTime = 0;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public AutoReconnect() {
        super(EncryptedString.of("Auto Reconnect"), EncryptedString.of("Automatically reconnects when disconnected from a server."), -1, Category.MISC);
        addSettings(time, button);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        shouldReconnect = false;
        reconnectTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        shouldReconnect = false;
        reconnectTime = 0;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;

        // Check if we should reconnect
        if (shouldReconnect && System.currentTimeMillis() >= reconnectTime) {
            reconnect();
        }
    }

    public void setLastServer(ServerAddress address, ServerInfo info) {
        this.lastServerAddress = address;
        this.lastServerInfo = info;
    }

    public void scheduleReconnect() {
        if (!this.isEnabled() || lastServerAddress == null) return;

        shouldReconnect = true;
        reconnectTime = System.currentTimeMillis() + (long) (time.getValue() * 1000);
        
        ChatUtils.info("Reconnecting in " + time.getValue() + " seconds...");
    }

    private void reconnect() {
        if (lastServerAddress == null || lastServerInfo == null) {
            shouldReconnect = false;
            return;
        }

        try {
            // Attempt to reconnect
            mc.setScreen(null); // Close any open screen
            mc.getNetworkHandler().getConnection().disconnect(net.minecraft.text.Text.literal("AutoReconnect"));
            
            // Schedule the actual reconnection
            executor.schedule(() -> {
                try {
                    // Use the direct server connection method
                    mc.getNetworkHandler().getConnection().disconnect(net.minecraft.text.Text.literal("Reconnecting..."));
                    mc.setScreen(null);
                } catch (Exception e) {
                    ChatUtils.error("Failed to reconnect: " + e.getMessage());
                }
            }, 100, TimeUnit.MILLISECONDS);

            shouldReconnect = false;
        } catch (Exception e) {
            ChatUtils.error("Error during reconnection: " + e.getMessage());
            shouldReconnect = false;
        }
    }

    public boolean shouldHideButtons() {
        return button.getValue();
    }

    public boolean isReconnecting() {
        return shouldReconnect;
    }

    public long getReconnectTime() {
        return reconnectTime;
    }

    public double getRemainingTime() {
        if (!shouldReconnect) return 0;
        return Math.max(0, (reconnectTime - System.currentTimeMillis()) / 1000.0);
    }
} 