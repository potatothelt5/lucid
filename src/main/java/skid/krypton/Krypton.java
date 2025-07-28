package skid.krypton;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import skid.krypton.gui.ClickGUI;
import skid.krypton.manager.EventManager;
import skid.krypton.module.ModuleManager;
import skid.krypton.utils.ChatUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

public final class Krypton {
    public ModuleManager MODULE_MANAGER;
    public EventManager EVENT_BUS;
    public static MinecraftClient mc;
    public String version;
    public static Krypton INSTANCE;
    public boolean shouldPreventClose;
    public ClickGUI GUI;
    public Screen screen;
    public long modified;
    public File jar;
    private SimpleConfigManager simpleConfigManager;

    public Krypton() {
        try {
            Krypton.INSTANCE = this;
            this.version = " b1.3";
            this.screen = null;
            this.EVENT_BUS = new EventManager();
            this.MODULE_MANAGER = new ModuleManager();
            this.GUI = new ClickGUI();
            this.jar = new File(Krypton.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            this.modified = this.jar.lastModified();
            this.shouldPreventClose = false;
            Krypton.mc = MinecraftClient.getInstance();
            // Initialize ChatUtils
            ChatUtils.init();
            // Create Krypton directories
            this.createKryptonDirectories();
            // Config manager
            this.simpleConfigManager = new SimpleConfigManager(this.MODULE_MANAGER);
            this.simpleConfigManager.load();
        } catch (Throwable _t) {
            _t.printStackTrace(System.err);
        }
    }

    public ModuleManager getModuleManager() {
        return this.MODULE_MANAGER;
    }

    public EventManager getEventBus() {
        return this.EVENT_BUS;
    }

    private void createKryptonDirectories() {
        try {
            String kryptonDir = System.getProperty("user.home") + "/Krypton";
            String soundsDir = kryptonDir + "/sounds";
            
            File kryptonFolder = new File(kryptonDir);
            File soundsFolder = new File(soundsDir);
            
            if (!kryptonFolder.exists()) {
                kryptonFolder.mkdirs();
            }
            if (!soundsFolder.exists()) {
                soundsFolder.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetModifiedDate() {
        this.jar.setLastModified(this.modified);
    }

    public void shutdown() {
        if (this.simpleConfigManager != null) {
            this.simpleConfigManager.save();
        }
    }

    // SimpleConfigManager implementation
    public static class SimpleConfigManager {
        private static final String CONFIG_PATH = System.getProperty("user.home") + "/Krypton/config.json";
        private final ModuleManager moduleManager;
        private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        public SimpleConfigManager(ModuleManager moduleManager) {
            this.moduleManager = moduleManager;
        }

        public void load() {
            try {
                File file = new File(CONFIG_PATH);
                if (!file.exists()) return;
                JsonObject root = gson.fromJson(new FileReader(file), JsonObject.class);
                for (skid.krypton.module.Module module : moduleManager.getModules()) {
                    JsonObject modObj = root.has(module.getName().toString()) ? root.getAsJsonObject(module.getName().toString()) : null;
                    if (modObj == null) continue;
                    if (modObj.has("enabled") && modObj.get("enabled").getAsBoolean()) {
                        module.toggle(true);
                    }
                    for (skid.krypton.module.setting.Setting setting : module.getSettings()) {
                        if (!modObj.has(setting.getName().toString())) continue;
                        JsonElement val = modObj.get(setting.getName().toString());
                        if (setting instanceof skid.krypton.module.setting.BooleanSetting) {
                            ((skid.krypton.module.setting.BooleanSetting) setting).setValue(val.getAsBoolean());
                        } else if (setting instanceof skid.krypton.module.setting.NumberSetting) {
                            ((skid.krypton.module.setting.NumberSetting) setting).getValue(val.getAsDouble());
                        } else if (setting instanceof skid.krypton.module.setting.StringSetting) {
                            ((skid.krypton.module.setting.StringSetting) setting).setValue(val.getAsString());
                        } // Add more as needed
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void save() {
            try {
                File file = new File(CONFIG_PATH);
                file.getParentFile().mkdirs();
                JsonObject root = new JsonObject();
                for (skid.krypton.module.Module module : moduleManager.getModules()) {
                    JsonObject modObj = new JsonObject();
                    modObj.addProperty("enabled", module.isEnabled());
                    for (skid.krypton.module.setting.Setting setting : module.getSettings()) {
                        if (setting instanceof skid.krypton.module.setting.BooleanSetting) {
                            modObj.addProperty(setting.getName().toString(), ((skid.krypton.module.setting.BooleanSetting) setting).getValue());
                        } else if (setting instanceof skid.krypton.module.setting.NumberSetting) {
                            modObj.addProperty(setting.getName().toString(), ((skid.krypton.module.setting.NumberSetting) setting).getValue());
                        } else if (setting instanceof skid.krypton.module.setting.StringSetting) {
                            modObj.addProperty(setting.getName().toString(), ((skid.krypton.module.setting.StringSetting) setting).getValue());
                        } // Add more as needed
                    }
                    root.add(module.getName().toString(), modObj);
                }
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(root, writer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
