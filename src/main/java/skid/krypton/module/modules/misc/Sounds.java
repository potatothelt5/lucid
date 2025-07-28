package skid.krypton.module.modules.misc;

import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.AttackEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.system.FileUtils;

import java.io.File;

public final class Sounds extends Module {
    private final BooleanSetting killSound = new BooleanSetting(EncryptedString.of("Kill Sound"), true)
            .setDescription(EncryptedString.of("Play sounds when you kill a player"));
    private final NumberSetting killVolume = new NumberSetting(EncryptedString.of("Kill Volume"), 0.0, 1.0, 1.0, 0.1)
            .getValue(EncryptedString.of("The volume for the kill sounds"));
    private final StringSetting killName = new StringSetting(EncryptedString.of("Kill Sound File"), "killsound.wav")
            .setDescription(EncryptedString.of("The name of the kill sound file"));

    private final BooleanSetting hitSound = new BooleanSetting(EncryptedString.of("Hit Sound"), true)
            .setDescription(EncryptedString.of("Play sounds when you hit an entity"));
    private final NumberSetting hitVolume = new NumberSetting(EncryptedString.of("Hit Volume"), 0.0, 1.0, 1.0, 0.1)
            .getValue(EncryptedString.of("The volume for the hit sounds"));
    private final StringSetting hitName = new StringSetting(EncryptedString.of("Hit Sound File"), "hitsound.wav")
            .setDescription(EncryptedString.of("The name of the hit sound file"));

    public Sounds() {
        super(EncryptedString.of("Sounds"), EncryptedString.of("Plays custom sounds when something happens"), -1, Category.MISC);
        this.addSettings(killSound, killVolume, killName, hitSound, hitVolume, hitName);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // Create Krypton directories if they don't exist
        String kryptonDir = System.getProperty("user.home") + "/Krypton";
        String soundsDir = kryptonDir + "/sounds";
        FileUtils.createDirectoriesIfNotExist(kryptonDir);
        FileUtils.createDirectoriesIfNotExist(soundsDir);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventListener
    public void onAttack(AttackEvent event) {
        // Check if we're in a world/server
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        // Check if we're connected to a server or in singleplayer
        if (mc.getCurrentServerEntry() == null && mc.isInSingleplayer() == false) {
            return;
        }
        
        // Additional check: Make sure we're actually in a game world, not in a menu
        if (mc.currentScreen != null) {
            return;
        }
        
        // Check if we're actually in a loaded world
        if (mc.world.getRegistryKey() == null) {
            return;
        }
        
        if (!hitSound.getValue()) {
            return;
        }
        
        String soundPath = System.getProperty("user.home") + "/Krypton/sounds/" + hitName.getValue();
        File soundFile = new File(soundPath);
        
        if (!soundFile.exists()) {
            return;
        }
        
        FileUtils.playSound(soundFile, hitVolume.getFloatValue());
    }

    // Note: TargetDeathEvent doesn't exist in Krypton, so we'll need to handle player deaths differently
    // You can add a custom event or use existing death detection logic
    public void onPlayerKill(String playerName) {
        if (!isEnabled()) return;
        
        // Check if we're in a world/server
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        // Check if we're connected to a server or in singleplayer
        if (mc.getCurrentServerEntry() == null && mc.isInSingleplayer() == false) {
            return;
        }
        
        if (killSound.getValue()) {
            String soundPath = System.getProperty("user.home") + "/Krypton/sounds/" + killName.getValue();
            File soundFile = new File(soundPath);
            
            if (!soundFile.exists()) {
                return;
            }
            
            FileUtils.playSound(soundFile, killVolume.getFloatValue());
        }
    }
} 