package skid.krypton.module.modules.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class StaffDetector extends Module {
    
    // Detection settings
    private final BooleanSetting enabled = new BooleanSetting(EncryptedString.of("Enabled"), true).setDescription(EncryptedString.of("Enable staff detection"));
    private final NumberSetting detectionRange = new NumberSetting(EncryptedString.of("Detection Range"), 10.0, 200.0, 50.0, 1.0).getValue(EncryptedString.of("Range to detect staff members"));
    private final BooleanSetting notifyInChat = new BooleanSetting(EncryptedString.of("Notify in Chat"), true).setDescription(EncryptedString.of("Send notification in chat when staff is detected"));
    private final BooleanSetting autoDisconnect = new BooleanSetting(EncryptedString.of("Auto Disconnect"), false).setDescription(EncryptedString.of("Automatically disconnect when staff is detected"));
    private final BooleanSetting stopActions = new BooleanSetting(EncryptedString.of("Stop Actions"), true).setDescription(EncryptedString.of("Stop all actions when staff is detected"));
    private final BooleanSetting detectVanishSpectator = new BooleanSetting(EncryptedString.of("Detect Vanish/Spectator"), true).setDescription(EncryptedString.of("Detect staff in vanish or spectator mode"));
    
    // Staff name settings
    private final StringSetting staffName1 = new StringSetting(EncryptedString.of("Staff Name 1"), "staff").setDescription(EncryptedString.of("First staff name to detect"));
    private final StringSetting staffName2 = new StringSetting(EncryptedString.of("Staff Name 2"), "admin").setDescription(EncryptedString.of("Second staff name to detect"));
    private final StringSetting staffName3 = new StringSetting(EncryptedString.of("Staff Name 3"), "mod").setDescription(EncryptedString.of("Third staff name to detect"));
    private final StringSetting staffName4 = new StringSetting(EncryptedString.of("Staff Name 4"), "helper").setDescription(EncryptedString.of("Fourth staff name to detect"));
    private final StringSetting staffName5 = new StringSetting(EncryptedString.of("Staff Name 5"), "owner").setDescription(EncryptedString.of("Fifth staff name to detect"));
    private final StringSetting staffName6 = new StringSetting(EncryptedString.of("Staff Name 6"), "manager").setDescription(EncryptedString.of("Sixth staff name to detect"));
    private final StringSetting staffName7 = new StringSetting(EncryptedString.of("Staff Name 7"), "").setDescription(EncryptedString.of("Seventh staff name to detect"));
    private final StringSetting staffName8 = new StringSetting(EncryptedString.of("Staff Name 8"), "").setDescription(EncryptedString.of("Eighth staff name to detect"));
    
    // State variables
    private boolean staffNearby = false;
    private int lastCheck = 0;
    private List<String> detectedStaff = new ArrayList<>();
    private int notificationCooldown = 0;
    
    public StaffDetector() {
        super(EncryptedString.of("Staff Detector"), EncryptedString.of("Detects staff members and takes action"), -1, Category.MISC);
        this.addSettings(this.enabled, this.detectionRange, this.notifyInChat, this.autoDisconnect, this.stopActions, this.detectVanishSpectator,
                        this.staffName1, this.staffName2, this.staffName3, this.staffName4,
                        this.staffName5, this.staffName6, this.staffName7, this.staffName8);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        this.staffNearby = false;
        this.detectedStaff.clear();
        this.lastCheck = 0;
        this.notificationCooldown = 0;
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        if (!this.enabled.getValue() || mc.player == null || mc.world == null) {
            return;
        }
        
        // Check every 20 ticks (1 second)
        if (this.lastCheck > 0) {
            this.lastCheck--;
            return;
        }
        
        this.lastCheck = 20;
        this.checkForStaff();
        
        // Handle actions based on staff detection
        if (this.staffNearby) {
            this.handleStaffDetected();
        }
        
        // Notification cooldown
        if (this.notificationCooldown > 0) {
            this.notificationCooldown--;
        }
    }
    
    private void checkForStaff() {
        this.staffNearby = false;
        this.detectedStaff.clear();
        
        double range = this.detectionRange.getValue();
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Tablist UUIDs for vanish detection
        Set<String> tablistUuids = new HashSet<>();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry ple : mc.getNetworkHandler().getPlayerList()) {
                tablistUuids.add(ple.getProfile().getId().toString());
            }
        }
        
        // Check all players in the world
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            double distance = playerPos.getSquaredDistance(player.getBlockPos());
            if (distance > range * range) continue;
            String playerName = player.getName().getString();
            boolean isStaff = isStaffName(playerName);
            boolean detected = false;
            
            // Spectator detection
            if (this.detectVanishSpectator.getValue() && player.isSpectator() && isStaff) {
                detected = true;
                if (!this.detectedStaff.contains(playerName)) {
                    this.detectedStaff.add(playerName + " (Spectator)");
                }
                ChatUtils.info("Staff detected in spectator mode: " + playerName);
            }
            // Vanish detection: player in world but not in tablist
            else if (this.detectVanishSpectator.getValue() && isStaff && !tablistUuids.contains(player.getGameProfile().getId().toString())) {
                detected = true;
                if (!this.detectedStaff.contains(playerName)) {
                    this.detectedStaff.add(playerName + " (Vanish)");
                }
                ChatUtils.info("Staff detected in vanish: " + playerName);
            }
            // Normal detection
            else if (isStaff) {
                detected = true;
                if (!this.detectedStaff.contains(playerName)) {
                    this.detectedStaff.add(playerName);
                }
            }
            if (detected) {
                this.staffNearby = true;
            }
        }
    }
    
    private boolean isStaffName(String playerName) {
        String playerNameLower = playerName.toLowerCase();
        
        // Check against all configured staff names (case insensitive)
        String[] staffNames = {
            this.staffName1.getValue().toLowerCase(),
            this.staffName2.getValue().toLowerCase(),
            this.staffName3.getValue().toLowerCase(),
            this.staffName4.getValue().toLowerCase(),
            this.staffName5.getValue().toLowerCase(),
            this.staffName6.getValue().toLowerCase(),
            this.staffName7.getValue().toLowerCase(),
            this.staffName8.getValue().toLowerCase()
        };
        
        for (String staffName : staffNames) {
            if (!staffName.isEmpty() && playerNameLower.contains(staffName)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void handleStaffDetected() {
        // Send notification in chat
        if (this.notifyInChat.getValue() && this.notificationCooldown == 0) {
            StringBuilder message = new StringBuilder("§c[StaffDetector] §fStaff detected nearby: ");
            for (int i = 0; i < this.detectedStaff.size(); i++) {
                if (i > 0) message.append(", ");
                message.append("§c").append(this.detectedStaff.get(i));
            }
            
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.of(message.toString()));
            }
            
            this.notificationCooldown = 200; // 10 second cooldown
        }
        
        // Auto disconnect - always enabled when staff is detected
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.of("§c[StaffDetector] §fStaff detected! Leaving game..."));
            this.toggle();
            // Disconnect immediately
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Short delay to show message
                    if (mc.world != null) {
                        mc.world.disconnect();
                    }
                } catch (InterruptedException e) {
                    // Ignore
                }
            }).start();
        }
        
        // Stop all actions by disabling other modules
        if (this.stopActions.getValue()) {
            this.stopOtherModules();
        }
    }
    
    private void stopOtherModules() {
        // Only disable UndetectedTunnelBaseFinder when staff is detected
        try {
            Module undetectedTunnelFinder = Krypton.INSTANCE.MODULE_MANAGER.getModule(skid.krypton.module.modules.donut.UndetectedTunnelBaseFinder.class);
            
            if (undetectedTunnelFinder != null && undetectedTunnelFinder.isEnabled()) {
                undetectedTunnelFinder.toggle();
                if (mc.player != null) {
                    mc.player.sendMessage(net.minecraft.text.Text.of("§c[StaffDetector] §fDisabled UndetectedTunnelBaseFinder due to staff detection"));
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    /**
     * Public method to check if staff is nearby
     * @return true if staff is detected nearby
     */
    public boolean isStaffNearby() {
        return this.staffNearby;
    }
    
    /**
     * Public method to get list of detected staff members
     * @return List of detected staff member names
     */
    public List<String> getDetectedStaff() {
        return new ArrayList<>(this.detectedStaff);
    }
    
    /**
     * Public method to check if a specific player name is staff
     * @param playerName The player name to check
     * @return true if the player is considered staff
     */
    public boolean isPlayerStaff(String playerName) {
        return isStaffName(playerName);
    }
    
    /**
     * Public method to get the detection range
     * @return Current detection range
     */
    public double getDetectionRange() {
        return this.detectionRange.getValue();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        this.staffNearby = false;
        this.detectedStaff.clear();
    }
} 