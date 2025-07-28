package skid.krypton.module;

import net.minecraft.client.gui.screen.ChatScreen;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.KeyEvent;
import skid.krypton.module.modules.client.*;
import skid.krypton.module.modules.combat.*;
import skid.krypton.module.modules.donut.*;
import skid.krypton.module.modules.misc.*;
import skid.krypton.module.modules.render.*;
import skid.krypton.module.setting.BindSetting;
import skid.krypton.utils.EncryptedString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        addModules();
        addKeybinds();
    }

    public void addModules() {
        //Combat
        add(new AnchorMacro());
        add(new AutoCrystal());
        add(new AutoHitCrystal());
        add(new AutoInventoryTotem());
        add(new AutoJumpReset());
        add(new AutoTotem());
        add(new CrystalOptimizer());
        add(new DoubleAnchor());
        add(new ElytraSwap());
        add(new Hitbox());
        add(new HoverTotem());
        add(new NoHitDelay());
        add(new ShieldDisabler());
        add(new TotemOffhand());
        add(new MaceSwap());
        add(new StaticHitboxes());
        add(new TriggerBot());

        //Misc
        add(new AutoEat());
        add(new AutoFirework());
        add(new AutoLog());
        add(new AutoLoot());
        add(new AutoMine());
        add(new AutoReconnect());
        add(new AutoTool());
        add(new AutoTPA());
        add(new CordSnapper());
        add(new ElytraGlide());
        add(new FastPlace());
        add(new Freecam());
        add(new KeyPearl());
        add(new KeyWindCharge());
        add(new NameProtect());
        add(new PlayerDetection());
        add(new StaffDetector());
        add(new Sounds());
        add(new Notifications());
        add(new MouseFix());

        //Donut
        add(new AntiTrap());
        add(new AuctionSniper());
        add(new AutoSell());
        add(new AutoSpawnerSell());
        add(new AutoAmethystPickaxe());
        add(new BoneDropper());
        add(new NetheriteFinder());
        add(new RtpBaseFinder());
        add(new ShulkerDropper());
        add(new SpawnerProtect());
        add(new UndetectedTunnelBaseFinder());
        add(new TunnelBaseFinder());

        //Render
        add(new Fullbright());
        add(new HUD());
        add(new PlayerESP());
        add(new StorageESP());
        add(new TargetHUD());

        //Client
        add(new skid.krypton.module.modules.client.Krypton());
        add(new SelfDestruct());
        add(new Friends());
    }

    public List<Module> getEnabledModules() {
        return modules.stream()
                .filter(Module::isEnabled)
                .toList();
    }

    public List<Module> getModules() {
        return modules;
    }

    public void addKeybinds() {
        Krypton.INSTANCE.getEventBus().register(this);

        for (Module module : modules) {
            module.addSetting(new BindSetting(EncryptedString.of("Keybind"), module.getKeybind(), true).setDescription(EncryptedString.of("Key to enabled the module")));
        }
    }

    public List<Module> getModulesInCategory(Category category) {
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> moduleClass) {
        return (T) modules.stream()
                .filter(moduleClass::isInstance)
                .findFirst()
                .orElse(null);
    }

    public void add(Module module) {
        Krypton.INSTANCE.getEventBus().register(module);
        modules.add(module);
    }

    @EventListener
    public void onKeyPress(KeyEvent event) {
        if (Krypton.mc.player == null || Krypton.mc.currentScreen instanceof ChatScreen) {
            return;
        }

        if (!SelfDestruct.isActive) {
            modules.forEach(module -> {
                if (module.getKeybind() == event.key && event.mode == 1) {
                    module.toggle();
                }
            });
        }
    }
}
