package skid.krypton.module.modules.combat;

import net.minecraft.client.MinecraftClient;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.AttackEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;

public class NoHitDelay extends Module {
    
    public final BooleanSetting resetCooldown = new BooleanSetting(EncryptedString.of("Reset Cooldown"), true);
    public final BooleanSetting instantAttack = new BooleanSetting(EncryptedString.of("Instant Attack"), true);
    public final NumberSetting cooldownValue = new NumberSetting(EncryptedString.of("Cooldown Value"), 0.0, 1.0, 0.0, 0.1);
    
    public NoHitDelay() {
        super(EncryptedString.of("No Hit Delay"), EncryptedString.of("Removes attack cooldown/hit delay"), -1, Category.COMBAT);
        addSettings(resetCooldown, instantAttack, cooldownValue);
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
    public void onAttack(AttackEvent event) {
        // Attack event handled by mixin
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        // Tick event handled by mixin
    }
} 