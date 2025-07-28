package skid.krypton.module.modules.render;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.ModeSetting;
import skid.krypton.utils.EncryptedString;

import java.lang.reflect.Field;

public class Fullbright extends Module {
    public enum Mode {
        GAMMA,
        EFFECT
    }

    private final ModeSetting<Mode> mode = new ModeSetting<>(EncryptedString.of("Mode"), Mode.GAMMA, Mode.class);
    private double oldGamma = -1;

    public Fullbright() {
        super(EncryptedString.of("Fullbright"), EncryptedString.of("Brightens the world (Gamma or Night Vision)"), -1, Category.RENDER);
        this.addSettings(this.mode);
    }

    @Override
    public void onEnable() {
        if (mode.getValue() == Mode.GAMMA) {
            try {
                // Try to access gamma through reflection since the API changed
                Field gammaField = mc.options.getClass().getDeclaredField("gamma");
                gammaField.setAccessible(true);
                Object gammaOption = gammaField.get(mc.options);
                
                // Get the current value
                Field valueField = gammaOption.getClass().getDeclaredField("value");
                valueField.setAccessible(true);
                oldGamma = (Double) valueField.get(gammaOption);
                
                // Set the new value
                valueField.set(gammaOption, 15.0);
            } catch (Exception e) {
                // Fallback to night vision if gamma access fails
                if (mc.player != null) {
                    mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000000, 0, false, false));
                }
            }
        } else if (mode.getValue() == Mode.EFFECT) {
            if (mc.player != null) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000000, 0, false, false));
            }
        }
    }

    @Override
    public void onDisable() {
        if (mode.getValue() == Mode.GAMMA && oldGamma >= 0) {
            try {
                // Restore the original gamma value
                Field gammaField = mc.options.getClass().getDeclaredField("gamma");
                gammaField.setAccessible(true);
                Object gammaOption = gammaField.get(mc.options);
                
                Field valueField = gammaOption.getClass().getDeclaredField("value");
                valueField.setAccessible(true);
                valueField.set(gammaOption, oldGamma);
            } catch (Exception e) {
                // If gamma restoration fails, just remove night vision
                if (mc.player != null) {
                    mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
            }
        } else if (mode.getValue() == Mode.EFFECT) {
            if (mc.player != null) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        }
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;
        
        if (mc.player == null) return;
        
        if (mode.getValue() == Mode.EFFECT) {
            StatusEffectInstance eff = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (eff == null || eff.getDuration() < 220) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000000, 0, false, false));
            }
        } else if (mode.getValue() == Mode.GAMMA) {
            try {
                // Ensure gamma stays at fullbright level
                Field gammaField = mc.options.getClass().getDeclaredField("gamma");
                gammaField.setAccessible(true);
                Object gammaOption = gammaField.get(mc.options);
                
                Field valueField = gammaOption.getClass().getDeclaredField("value");
                valueField.setAccessible(true);
                double currentGamma = (Double) valueField.get(gammaOption);
                
                // If gamma is not at fullbright level, set it
                if (currentGamma < 15.0) {
                    valueField.set(gammaOption, 15.0);
                }
            } catch (Exception e) {
                // If gamma access fails, fallback to night vision
                StatusEffectInstance eff = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (eff == null || eff.getDuration() < 220) {
                    mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000000, 0, false, false));
                }
            }
        }
    }
} 