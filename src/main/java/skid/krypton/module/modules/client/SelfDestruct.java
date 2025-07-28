package skid.krypton.module.modules.client;

import com.sun.jna.Memory;
import skid.krypton.gui.ClickGUI;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.Setting;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

public final class SelfDestruct extends Module {
    public static boolean isActive = false;
    private final BooleanSetting replaceMod = new BooleanSetting(EncryptedString.of("Replace Mod"), true).setDescription(EncryptedString.of("Repalces the mod with the original JAR file of the ImmediatelyFast mod"));
    private final BooleanSetting saveLastModified = new BooleanSetting(EncryptedString.of("Save Last Modified"), true).setDescription(EncryptedString.of("Saves the last modified date after self destruct"));
    private final StringSetting replaceUrl = new StringSetting(EncryptedString.of("Replace URL"), "https://cdn.modrinth.com/data/8shC1gFX/versions/sXO3idkS/BetterF3-11.0.1-Fabric-1.21.jar");
    private static final Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
    private static final AtomicLong modificationCounter = new AtomicLong();

    public SelfDestruct() {
        super(EncryptedString.of("Self Destruct"), EncryptedString.of("Removes the client from your game |Credits to Argon for deletion|"), -1, Category.CLIENT);
        this.addSettings(this.replaceMod, this.saveLastModified, this.replaceUrl);
    }

    @Override
    public void onEnable() {
        isActive = true;
        skid.krypton.Krypton.INSTANCE.getModuleManager().getModule(Krypton.class).toggle(false);
        this.toggle(false);
        skid.krypton.Krypton.INSTANCE.shutdown();
        if (this.mc.currentScreen instanceof ClickGUI) {
            skid.krypton.Krypton.INSTANCE.shouldPreventClose = false;
            this.mc.currentScreen.close();
        }
        if (this.replaceMod.getValue()) {
            try {
                String string = this.replaceUrl.getValue();
                if (Utils.getCurrentJarPath().exists()) {
                    Utils.overwriteFile(string, Utils.getCurrentJarPath());
                }
            }
            catch (Exception ignored) {}
        }
        for (Module module : skid.krypton.Krypton.INSTANCE.getModuleManager().getModules()) {
            module.toggle(false);
            module.setName(null);
            module.setDescription(null);
            for (Setting setting : module.getSettings()) {
                setting.getDescription(null);
                setting.setDescription(null);
                if (!(setting instanceof StringSetting)) continue;
                ((StringSetting) setting).setValue(null);
            }
            module.getSettings().clear();
        }
        Runtime runtime = Runtime.getRuntime();
        if (this.saveLastModified.getValue()) {
            skid.krypton.Krypton.INSTANCE.resetModifiedDate();
        }
        for (int i = 0; i <= 10; ++i) {
            runtime.gc();
            runtime.runFinalization();
            try {
                Thread.sleep(100 * i);
                Memory.purge();
                Memory.disposeAll();
                continue;
            }
            catch (InterruptedException interruptedException) {}
        }
    }

}
